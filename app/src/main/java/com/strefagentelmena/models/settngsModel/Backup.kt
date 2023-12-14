package com.strefagentelmena.models.settngsModel

import com.strefagentelmena.models.AppoimentsModel.Appointment
import com.strefagentelmena.models.Customer

class Backup(
    var customersCopy: List<Customer> = emptyList(),
    var appoimentsCopy: List<Appointment> = emptyList(),
    var profileCopy: ProfilePreferences = ProfilePreferences()
)