package com.pedilo.app.core.runtime

import com.pedilo.app.core.firebase.FirebaseTeamAccessAdapter
import com.pedilo.app.core.port.TeamAccessPort

fun teamAccessPort(): TeamAccessPort = FirebaseTeamAccessAdapter()
