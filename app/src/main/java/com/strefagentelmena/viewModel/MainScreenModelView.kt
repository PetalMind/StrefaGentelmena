package com.strefagentelmena.viewModel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.strefagentelmena.enums.AppState
import com.strefagentelmena.functions.Greetings
import com.strefagentelmena.functions.isNotificationSendWindow
import com.strefagentelmena.functions.fireBase.FirebaseEmployeeFunctions
import com.strefagentelmena.functions.fireBase.FirebaseFunctionsAppointments
import com.strefagentelmena.functions.fireBase.FirebaseProfilePreferences
import com.strefagentelmena.models.appoimentsModel.Appointment
import com.strefagentelmena.models.appoimentsModel.appointmentIntervalEndExclusiveMinutes
import com.strefagentelmena.models.appoimentsModel.parseAppointmentTimeString
import com.strefagentelmena.models.Customer
import com.strefagentelmena.models.computeSalonAnalytics
import com.strefagentelmena.models.mergeCustomersWithVisitStats
import com.strefagentelmena.models.normalizedAfterFirebaseLoad
import com.strefagentelmena.models.settngsModel.Employee
import com.strefagentelmena.models.settngsModel.ProfilePreferences
import com.strefagentelmena.models.settngsModel.vacationRangeLabel
import com.strefagentelmena.models.settngsModel.vacationRangeOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.time.format.DateTimeFormatter
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class MainScreenModelView : ViewModel() {
    val messages = MutableLiveData("")
    val viewState = MutableLiveData(AppState.Idle)
    val customersLists = MutableLiveData<List<Customer>>(emptyList())
    private val appointmentsLists = MutableLiveData<List<Appointment>>(listOf(Appointment()))
    val appointmentsForAnalytics: LiveData<List<Appointment>> = appointmentsLists
    val appointmentsToNotify = MutableLiveData<List<Appointment>>(emptyList())
    val showNotifyDialog = MutableLiveData(false)
    val upcomingAppointment: MutableLiveData<Appointment> = MutableLiveData(Appointment())
    /** Wizyty trwające teraz (przedział [start, end)), posortowane wg startu. */
    val ongoingVisitsNow: MutableLiveData<List<Appointment>> = MutableLiveData(emptyList())
    /** Wizyty już zakończone, których koniec był nie wcześniej niż 24 h temu — od najnowszego końca. */
    val recentVisitsLast24h: MutableLiveData<List<Appointment>> = MutableLiveData(emptyList())
    val extendVisitInProgress: MutableLiveData<Boolean> = MutableLiveData(false)
    val profilePreferences = MutableLiveData(ProfilePreferences())
    /** Liczba wizyt z dzisiejszą datą (dd.MM.yyyy), po załadowaniu harmonogramu. */
    val todayAppointmentsCount = MutableLiveData(0)
    /** Liczba wizyt (id ≠ 0) wg dnia tygodnia: indeks 0 = poniedziałek bieżącego tygodnia ISO. */
    val weekVisitCountsByDay = MutableLiveData(List(7) { 0 })
    val displayGreetings: MutableLiveData<String> = MutableLiveData(
        Greetings().getSeasonalAndPartOfDayGreeting(
            profilePreferences.value?.userName ?: "Użytkownik"
        )
    )
    private val _deferNotificationUntil = MutableLiveData<LocalDateTime?>()
    val deferNotificationUntil: LiveData<LocalDateTime?> = _deferNotificationUntil

    /** Tekst baneru na dashboard (pusty = brak aktywnych / zbliżających się urlopów). */
    val vacationDashboardReminder = MutableLiveData("")

    /** Lista pracowników do filtrowania statystyk itp. */
    val employees = MutableLiveData<List<Employee>>(emptyList())

    /** Statystyki salonu (ekran „Statystyki i analizy”) — odświeżane z klientów i wizyt. */
    val salonAnalytics = MutableLiveData(
        computeSalonAnalytics(emptyList(), emptyList()),
    )

    /** Wybrany pracownik do statystyk (null = cały salon). */
    val selectedEmployeeForAnalytics = MutableLiveData<Employee?>(null)

    fun setSelectedEmployeeForAnalytics(employee: Employee?) {
        selectedEmployeeForAnalytics.value = employee
        refreshSalonAnalytics()
    }

    fun deferNotifyDialog(minutes: Long) {
        _deferNotificationUntil.value = LocalDateTime.now().plusMinutes(minutes)
    }

    fun shouldShowNotifyDialog(): Boolean {
        val deferUntil = _deferNotificationUntil.value
        return deferUntil == null || LocalDateTime.now().isAfter(deferUntil)
    }

    fun setViewState(viewState: AppState) {
        this.viewState.value = viewState
    }

    fun newMessage(message: String) {
        messages.value = message
    }

    fun clearMessage() {
        messages.value = ""
    }

    fun setNotifyDialogVisible(visible: Boolean) {
        if (showNotifyDialog.value != visible) {
            showNotifyDialog.value = visible
        }
    }

    fun clearAppointmentsToNotify() {
        appointmentsToNotify.value = emptyList()
    }

    /** Numer rodzica przy wizycie zapisanej na dziecko — jak w harmonogramie. */
    fun resolveSmsContactPhoneForAppointment(ap: Appointment): String? {
        val parentId = ap.customer.parentCustomerId.takeIf { it > 0 }
            ?: ap.smsContactCustomerId.takeIf { it > 0 }
            ?: return null
        return customersLists.value
            ?.firstOrNull { it.id == parentId }
            ?.phoneNumber
            ?.takeIf { it.isNotBlank() }
    }

    private fun setAppointmentsList(value: List<Appointment>) {
        appointmentsLists.value = value
        val today = LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
        todayAppointmentsCount.value = value.count { it.id != 0 && it.date == today }
        weekVisitCountsByDay.value = computeWeekVisitCountsByDay(value)
        refreshSalonAnalytics()
    }

    private fun computeWeekVisitCountsByDay(appointments: List<Appointment>): List<Int> {
        val df = DateTimeFormatter.ofPattern("dd.MM.yyyy")
        val weekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val counts = IntArray(7)
        for (appt in appointments) {
            if (appt.id == 0) continue
            try {
                val d = LocalDate.parse(appt.date.trim(), df)
                val idx = ChronoUnit.DAYS.between(weekStart, d).toInt()
                if (idx in 0..6) counts[idx]++
            } catch (_: Exception) {
            }
        }
        return counts.toList()
    }

    private fun setCustomersList(customerList: List<Customer>) {
        customersLists.value = customerList
        refreshSalonAnalytics()
    }

    private fun refreshSalonAnalytics() {
        salonAnalytics.value = computeSalonAnalytics(
            customers = customersLists.value.orEmpty(),
            appointments = appointmentsLists.value.orEmpty(),
            employeeId = selectedEmployeeForAnalytics.value?.id,
        )
    }

    private fun findCustomerByName(name: String): Customer? {
        return customersLists.value?.firstOrNull { it.fullName == name }
    }

    private fun setUpcomingAppointment(appointment: Appointment) {
        upcomingAppointment.value = appointment
    }

    private fun setOngoingVisitsNow(visits: List<Appointment>) {
        ongoingVisitsNow.value = visits
    }

    private fun setRecentVisitsLast24h(visits: List<Appointment>) {
        recentVisitsLast24h.value = visits
    }

    /**
     * Odświeża tylko „kto następny” i „kto na wizycie” z już załadowanej listy (bez sieci).
     */
    fun refreshDashboardTimeSlots() {
        setUpcomingAppointment(computeUpcomingAppointmentForDashboard())
        setOngoingVisitsNow(computeOngoingVisitsForDashboard())
        setRecentVisitsLast24h(computeRecentVisitsLast24h())
    }

    fun extendVisitByMinutes(appointment: Appointment, extendByMinutes: Int) {
        if (appointment.id == 0) return
        val start = parseAppointmentTimeString(appointment.startTime)
        val end = parseAppointmentTimeString(appointment.endTime)
        if (start == null || end == null) {
            newMessage("Nie można odczytać godzin wizyty.")
            return
        }
        val (s0, e0) = appointmentIntervalEndExclusiveMinutes(start, end)
        if (e0 <= s0) {
            newMessage("Nie można przedłużyć wizyty o zerowym czasie.")
            return
        }
        val newEnd = start.plusMinutes((e0 - s0 + extendByMinutes).toLong())
        val newEndStr = newEnd.format(DateTimeFormatter.ofPattern("HH:mm"))
        val updated = appointment.copy(endTime = newEndStr)
        extendVisitInProgress.value = true
        FirebaseFunctionsAppointments().editAppointmentInFirebase(
            FirebaseDatabase.getInstance(),
            updated,
        ) { success ->
            viewModelScope.launch(Dispatchers.Main) {
                extendVisitInProgress.value = false
                if (success) {
                    newMessage("Wizyta przedłużona o $extendByMinutes min (do $newEndStr).")
                    checkAppointments()
                } else {
                    newMessage("Nie udało się zapisać przedłużenia wizyty.")
                }
            }
        }
    }

    private fun setAppointmentsToNotify(emptyList: List<Appointment>) {
        appointmentsToNotify.value = emptyList
    }

    private fun setGreetingRandom(greeting: String) {
        viewModelScope.launch(Dispatchers.Main) {
            displayGreetings.value = greeting
        }
    }

    /*
        private fun addFirstEmployee(context: Context) {
         //   val employees = fileFunctionsSettings.loadSettingsFromFile(context)

            val newEmployeeList = employees.employeeMutableList.toMutableList()
            if (employees.employeeMutableList.isEmpty()) {
                newEmployeeList.add(Employee(1, "Kinga", "Kloss"))
                profilePreferences.value?.employeeMutableList = newEmployeeList

                profilePreferences.value?.let {
                    fileFunctionsSettings.saveSettingsToFile(
                        context,
                        preferences = it
                    )
                }

            }

        }

    */


    /**
     * load Customer from firebase
     * @param database FirebaseDatabase
     * @return List of customers
     */
    private suspend fun loadCustomersList(database: FirebaseDatabase): List<Customer> {
        return suspendCoroutine { continuation ->
            val customersRef = database.getReference("Customers")
            customersRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val customers =
                        snapshot.children.mapNotNull {
                            it.getValue(Customer::class.java)
                                ?.takeUnless(Customer::deleted)
                                ?.normalizedAfterFirebaseLoad()
                        }
                    continuation.resume(customers) // Wznawia coroutine z wynikiem
                }

                override fun onCancelled(error: DatabaseError) {
                    continuation.resumeWithException(error.toException()) // Wznawia coroutine z wyjątkiem
                }
            })
        }
    }


    private suspend fun loadApointmentsList(): List<Appointment> {
        // Zwróć załadowanych klientów
        return FirebaseFunctionsAppointments().loadAppointmentsFromFirebase(
            FirebaseDatabase.getInstance()
        )
    }


    private fun sendNotificationsForUpcomingAppointments() {
        val currentTime = LocalDateTime.now()
        val today = LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
        val nextDay = LocalDate.now().plusDays(1).format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))

        val appointmentsToSend = appointmentsLists.value
            ?.filter { appointment ->
                !appointment.notificationSent && shouldSendNotificationSafe(
                    appointment,
                    today,
                    nextDay,
                    currentTime
                )
            }
            .orEmpty() // Gwarantujemy, że wynik nie będzie `null`

        if (appointmentsToSend.isNotEmpty()) {
            // Ustawienie listy spotkań do powiadomienia
            setAppointmentsToNotify(appointmentsToSend)

            messages.value = "Znaleziono ${appointmentsToSend.size} spotkań do powiadomienia"
            // Opcjonalnie: komunikat o znalezionych spotkaniach
            Log.d("Notifications", "Znaleziono ${appointmentsToSend.size} spotkań do powiadomienia")
        }
    }

    private fun shouldSendNotificationSafe(
        appointment: Appointment,
        today: String,
        nextDay: String,
        currentTime: LocalDateTime
    ): Boolean {
        return try {
            shouldSendNotificationForAppointment(appointment, today, nextDay, currentTime)
        } catch (e: Exception) {
            Log.e(
                "Notification Error",
                "Error parsing appointment: ${appointment.date} ${appointment.startTime}",
                e
            )
            messages.value = "Błąd parsowania daty spotkania: ${appointment.date}"
            false
        }
    }


    private fun shouldSendNotificationForAppointment(
        appointment: Appointment,
        today: String,
        nextDay: String,
        currentTime: LocalDateTime
    ): Boolean {
        if (appointment.notificationSent) return false

        val timeFmt = DateTimeFormatter.ofPattern("HH:mm")
        val visitStart = LocalTime.parse(appointment.startTime, timeFmt)

        val isToday = appointment.date == today &&
            currentTime.toLocalTime().isBefore(visitStart) &&
            shouldSendNotification(0, currentTime)

        val isTomorrow = appointment.date == nextDay &&
            shouldSendNotification(1, currentTime)

        return isToday || isTomorrow
    }


    private fun shouldSendNotification(daysDifference: Int, currentTime: LocalDateTime): Boolean {
        val startTime = profilePreferences.value?.notificationSendStartTime
        val endTime = profilePreferences.value?.notificationSendEndTime

        if (startTime.isNullOrEmpty() || endTime.isNullOrEmpty()) {
            Log.e("Notification Error", "Notification time range is not set in preferences.")
            return false
        }

        return try {
            val startLocalTime = LocalTime.parse(startTime, DateTimeFormatter.ofPattern("HH:mm"))
            val endLocalTime = LocalTime.parse(endTime, DateTimeFormatter.ofPattern("HH:mm"))

            (daysDifference == 1 || daysDifference == 0) &&
                isNotificationSendWindow(currentTime.toLocalTime(), startLocalTime, endLocalTime)
        } catch (e: Exception) {
            Log.e(
                "Notification Error",
                "Error parsing notification time range: $startTime - $endTime",
                e
            )
            false
        }
    }


    /**
     * Karta „Kto następny…”: tylko wizyta, której start jest w najbliższej godzinie (po teraz, przed teraz+1h).
     * Bez wizyty w tym oknie → pusta karta (id == 0).
     */
    private fun computeUpcomingAppointmentForDashboard(now: LocalDateTime = LocalDateTime.now()): Appointment {
        val appointments = appointmentsLists.value.orEmpty()
        if (appointments.isEmpty()) return Appointment()

        val dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")

        data class Parsed(val start: LocalDateTime, val appt: Appointment)

        val parsed = appointments.mapNotNull { appt ->
            if (appt.id == 0) return@mapNotNull null
            try {
                Parsed(
                    LocalDateTime.parse("${appt.date} ${appt.startTime}", dateTimeFormatter),
                    appt
                )
            } catch (e: Exception) {
                Log.w("Appointment", "Pominięto wizytę przy szukaniu nadchodzącej: ${appt.date} ${appt.startTime}", e)
                null
            }
        }

        val oneHourLater = now.plusHours(1)
        return parsed
            .filter { it.start.isAfter(now) && it.start.isBefore(oneHourLater) }
            .minByOrNull { it.start }
            ?.appt
            ?: Appointment()
    }

    /**
     * Wizyty, w których jesteśmy w przedziale [start, end) — z obsługą końca po północy.
     */
    private fun computeOngoingVisitsForDashboard(now: LocalDateTime = LocalDateTime.now()): List<Appointment> {
        val appointments = appointmentsLists.value.orEmpty()
        if (appointments.isEmpty()) return emptyList()
        val df = DateTimeFormatter.ofPattern("dd.MM.yyyy")
        data class Parsed(val start: LocalDateTime, val end: LocalDateTime, val appt: Appointment)
        val parsed = appointments.mapNotNull { appt ->
            if (appt.id == 0) return@mapNotNull null
            val startT = parseAppointmentTimeString(appt.startTime) ?: return@mapNotNull null
            val endT = parseAppointmentTimeString(appt.endTime) ?: return@mapNotNull null
            try {
                val d = LocalDate.parse(appt.date, df)
                val startDt = LocalDateTime.of(d, startT)
                var endDt = LocalDateTime.of(d, endT)
                if (!endDt.isAfter(startDt)) {
                    endDt = endDt.plusDays(1)
                }
                if (!endDt.isAfter(startDt)) return@mapNotNull null
                Parsed(startDt, endDt, appt)
            } catch (e: Exception) {
                Log.w(
                    "Appointment",
                    "Pominięto wizytę przy szukaniu trwających: ${appt.date} ${appt.startTime}",
                    e,
                )
                null
            }
        }
        return parsed
            .filter { p ->
                (now.isEqual(p.start) || now.isAfter(p.start)) && now.isBefore(p.end)
            }
            .sortedBy { it.start }
            .map { it.appt }
    }

    /**
     * Wizyty zakończone (teraz ≥ koniec), które skończyły się w oknie (teraz − 24h, teraz].
     */
    private fun computeRecentVisitsLast24h(now: LocalDateTime = LocalDateTime.now()): List<Appointment> {
        val appointments = appointmentsLists.value.orEmpty()
        if (appointments.isEmpty()) return emptyList()
        val df = DateTimeFormatter.ofPattern("dd.MM.yyyy")
        val windowStart = now.minusHours(24)
        data class Parsed(val end: LocalDateTime, val appt: Appointment)
        val parsed = appointments.mapNotNull { appt ->
            if (appt.id == 0) return@mapNotNull null
            val startT = parseAppointmentTimeString(appt.startTime) ?: return@mapNotNull null
            val endT = parseAppointmentTimeString(appt.endTime) ?: return@mapNotNull null
            try {
                val d = LocalDate.parse(appt.date, df)
                val startDt = LocalDateTime.of(d, startT)
                var endDt = LocalDateTime.of(d, endT)
                if (!endDt.isAfter(startDt)) {
                    endDt = endDt.plusDays(1)
                }
                if (!endDt.isAfter(startDt)) return@mapNotNull null
                Parsed(endDt, appt)
            } catch (e: Exception) {
                Log.w(
                    "Appointment",
                    "Pominięto wizytę przy szukaniu ostatnich: ${appt.date} ${appt.startTime}",
                    e,
                )
                null
            }
        }
        return parsed
            .filter { p ->
                !now.isBefore(p.end) && !p.end.isBefore(windowStart)
            }
            .sortedByDescending { it.end }
            .map { it.appt }
    }

    private fun setProfilePreferences(profile: ProfilePreferences) {
        profilePreferences.value = profile
    }

    fun startLoadingData() {
        setViewState(AppState.Loading)
        viewModelScope.launch {
            try {
                clearMessage()
                val database = FirebaseDatabase.getInstance()
                val customers = withContext(Dispatchers.IO) {
                    loadCustomersList(database)
                }
                val profile = withContext(Dispatchers.IO) {
                    FirebaseProfilePreferences().loadProfilePreferencesFromFirebase(database)
                }
                setProfilePreferences(profile)
                setCustomersList(customers)

                setViewState(AppState.Success)


                Log.i("Coroutine", "Loaded ${customers.size} customers.")
            } catch (e: Exception) {
                setViewState(AppState.Error)
                Log.e("Coroutine", "Error loading customers: ${e.message}", e)
            }
        }
    }

    fun checkAppointments() {
        viewModelScope.launch {
            try {
                // Pobranie listy spotkań w tle
                val appointments = withContext(Dispatchers.IO) {
                    loadApointmentsList()
                }
                val employeesLoaded = withContext(Dispatchers.IO) {
                    FirebaseEmployeeFunctions().loadEmployeesFromFirebase(FirebaseDatabase.getInstance())
                }
                employees.value = employeesLoaded
                vacationDashboardReminder.value = buildVacationDashboardReminderText(employeesLoaded)

                // Aktualizacja listy spotkań na głównym wątku
                setAppointmentsList(appointments)

                setCustomersList(
                    mergeCustomersWithVisitStats(
                        customersLists.value.orEmpty(),
                        appointments,
                    ),
                )

                setUpcomingAppointment(computeUpcomingAppointmentForDashboard())
                setOngoingVisitsNow(computeOngoingVisitsForDashboard())
                setRecentVisitsLast24h(computeRecentVisitsLast24h())

                // Wysłanie powiadomień o nadchodzących spotkaniach
                sendNotificationsForUpcomingAppointments()
            } catch (e: Exception) {
                // Obsługa błędów
                Log.e("CheckAppointments", "Error while checking appointments: ${e.message}", e)

                // Ustawienie stanu błędu v widoku
                setViewState(AppState.Error)
                messages.value = "Wystąpił błąd podczas sprawdzania spotkań."
            }
        }
    }

    /**
     * Urlopy przecinające się z [today, today + 14 dni] — przypomnienie na pulpicie.
     */
    private fun buildVacationDashboardReminderText(
        employees: List<Employee>,
        today: LocalDate = LocalDate.now(),
    ): String {
        val horizon = today.plusDays(14)
        val lines = employees.mapNotNull { emp ->
            val range = emp.vacationRangeOrNull() ?: return@mapNotNull null
            if (range.endInclusive.isBefore(today) || range.start.isAfter(horizon)) {
                return@mapNotNull null
            }
            val name = emp.displayName.ifBlank { "${emp.name} ${emp.surname}".trim() }.ifBlank { return@mapNotNull null }
            val label = emp.vacationRangeLabel()
            when {
                today in range -> "$name — teraz na urlopie ($label)"
                else -> "$name — zbliża się urlop ($label)"
            }
        }
        return lines.joinToString("\n")
    }

}
