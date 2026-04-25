package org.sova.navigation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.launch
import org.sova.data.AgentDeliberationApi
import org.sova.data.AgentDeliberationEvent
import org.sova.data.DisplayContent
import org.sova.data.PatientProfileApi
import org.sova.data.PatientProfilePersistence
import org.sova.data.VitalsApi
import org.sova.data.toAgentDeliberationStartRequest
import org.sova.data.toPatientProfilePayload
import org.sova.components.JournalTopBar
import org.sova.design.HealthColors
import org.sova.design.HealthShapes
import org.sova.design.HealthSpacing
import org.sova.logic.SimulationEngine
import org.sova.model.AgentDeliberationDecision
import org.sova.model.AgentDeliberationMessage
import org.sova.model.AgentDeliberationState
import org.sova.model.MedicalProfile
import org.sova.model.SimulationResult
import org.sova.model.UserProfile
import org.sova.model.Vitals
import org.sova.screens.AgentConversationScreen
import org.sova.screens.AgentsScreen
import org.sova.screens.DashboardScreen
import org.sova.screens.HistoryScreen
import org.sova.screens.OnboardingScreen
import org.sova.screens.ProfileScreen
import org.sova.screens.RecommendedActionScreen
import org.sova.screens.ShareWithCaregiverScreen
import org.sova.screens.SimulationScreen

@Composable
fun AppNavigation() {
    val patientId = remember { PatientProfilePersistence.patientId() }
    val coroutineScope = rememberCoroutineScope()
    var loaded by remember { mutableStateOf(false) }
    var onboarded by remember { mutableStateOf(false) }
    var route by remember { mutableStateOf(AppRoute.Home) }
    var simulationRun by remember { mutableStateOf(false) }
    var userProfile by remember { mutableStateOf<UserProfile?>(null) }
    var medicalProfile by remember { mutableStateOf<MedicalProfile?>(null) }
    var vitals by remember { mutableStateOf(Vitals()) }
    var syncMessage by remember { mutableStateOf<String?>(null) }
    var deliberationState by remember { mutableStateOf<AgentDeliberationState>(AgentDeliberationState.Idle) }
    var deliberationRefreshKey by remember { mutableStateOf(0) }

    LaunchedEffect(patientId) {
        val draft = PatientProfilePersistence.loadDraft()
        if (draft != null) {
            if (draft.hasRequiredFields()) {
                userProfile = draft.toUserProfile()
                medicalProfile = draft.toMedicalProfile()
                onboarded = true
                if (PatientProfileApi.sync(draft)) {
                    PatientProfilePersistence.clearDraft()
                    syncMessage = null
                } else {
                    syncMessage = "Saved locally. Sync will retry."
                }
            } else {
                onboarded = false
            }
        } else {
            val remote = PatientProfileApi.fetch(patientId)
            if (remote != null && remote.hasRequiredFields()) {
                userProfile = remote.toUserProfile()
                medicalProfile = remote.toMedicalProfile()
                onboarded = true
            }
        }
        val latestVitals = VitalsApi.latest(patientId)
        if (latestVitals == null) {
            println("Sova vitals: no database values available for patientId=$patientId.")
        } else {
            vitals = latestVitals.toVitals()
        }
        loaded = true
    }

    LaunchedEffect(loaded, onboarded, userProfile?.patientId, deliberationRefreshKey) {
        val user = userProfile ?: return@LaunchedEffect
        val medical = medicalProfile ?: MedicalProfile(emptyList(), emptyList(), emptyList())
        if (!loaded || !onboarded) return@LaunchedEffect

        deliberationState = AgentDeliberationState.Starting
        val messages = mutableListOf<AgentDeliberationMessage>()
        var convergence = 0.0
        var decision: AgentDeliberationDecision? = null
        var activeAgent: String? = null

        runCatching {
            val request = user.toAgentDeliberationStartRequest(medical, vitals)
            AgentDeliberationApi.start(request)
            deliberationState = AgentDeliberationState.Streaming(messages = emptyList())
            AgentDeliberationApi.observe(request.patientId).collect { event ->
                when (event) {
                    is AgentDeliberationEvent.Started -> {
                        deliberationState = AgentDeliberationState.Streaming(messages = messages.toList(), convergence = convergence)
                    }
                    is AgentDeliberationEvent.Message -> {
                        messages += event.value
                        activeAgent = event.value.agentName
                        deliberationState = AgentDeliberationState.Streaming(
                            messages = messages.toList(),
                            convergence = convergence,
                            activeAgent = activeAgent,
                        )
                    }
                    is AgentDeliberationEvent.Convergence -> {
                        convergence = event.value
                        deliberationState = AgentDeliberationState.Streaming(
                            messages = messages.toList(),
                            convergence = convergence,
                            activeAgent = activeAgent,
                        )
                    }
                    is AgentDeliberationEvent.Decision -> {
                        decision = event.value
                        deliberationState = AgentDeliberationState.Completed(messages = messages.toList(), decision = decision)
                    }
                    is AgentDeliberationEvent.Done -> {
                        deliberationState = AgentDeliberationState.Completed(messages = messages.toList(), decision = decision)
                    }
                    is AgentDeliberationEvent.Error -> {
                        deliberationState = AgentDeliberationState.Failed(event.message)
                    }
                }
            }
        }.onFailure {
            deliberationState = AgentDeliberationState.Failed("Agent service is unavailable. Retry when the backend is running.")
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(HealthColors.Background),
    ) {
        val isWide = maxWidth >= HealthSpacing.DesktopBreakpoint
        if (!loaded) {
            CenteredContent {
                Text("Loading Sova...", color = HealthColors.TextSecondary, style = MaterialTheme.typography.bodyLarge)
            }
        } else if (!onboarded) {
            Column(Modifier.fillMaxSize()) {
                CenteredChrome {
                    JournalTopBar()
                }
                CenteredContent(modifier = Modifier.weight(1f)) {
                    OnboardingScreen(
                        patientId = patientId,
                        onComplete = { user, medical ->
                            val payload = user.toPatientProfilePayload(medical)
                            PatientProfilePersistence.saveDraft(payload)
                            userProfile = user
                            medicalProfile = medical
                            onboarded = true
                            route = AppRoute.Home
                            coroutineScope.launch {
                                if (PatientProfileApi.sync(payload)) {
                                    PatientProfilePersistence.clearDraft()
                                    syncMessage = null
                                } else {
                                    syncMessage = "Saved locally. Sync will retry."
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        } else if (isWide) {
            Column(Modifier.fillMaxSize()) {
                CenteredChrome(wide = true) {
                    Column(verticalArrangement = Arrangement.spacedBy(HealthSpacing.Sm)) {
                        JournalTopBar()
                        DesktopTopNavigation(route = route, onRouteSelected = { route = it })
                    }
                }
                CenteredContent(modifier = Modifier.weight(1f), wide = true) {
                    RouteContent(
                        userProfile = userProfile,
                        medicalProfile = medicalProfile,
                        vitals = vitals,
                        route = route,
                        simulationRun = simulationRun,
                        deliberationState = deliberationState,
                        onRefreshDeliberation = { deliberationRefreshKey += 1 },
                        onProfileSave = { user, medical ->
                            val payload = user.toPatientProfilePayload(medical)
                            PatientProfilePersistence.saveDraft(payload)
                            userProfile = user
                            medicalProfile = medical
                            coroutineScope.launch {
                                if (PatientProfileApi.sync(payload)) {
                                    PatientProfilePersistence.clearDraft()
                                    syncMessage = null
                                } else {
                                    syncMessage = "Saved locally. Sync will retry."
                                }
                            }
                        },
                        onRoute = { route = it },
                    ) {
                            simulationRun = true
                            route = AppRoute.Simulation
                    }
                }
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                CenteredChrome {
                    JournalTopBar()
                }
                CenteredContent(modifier = Modifier.weight(1f)) {
                    RouteContent(
                        userProfile = userProfile,
                        medicalProfile = medicalProfile,
                        vitals = vitals,
                        route = route,
                        simulationRun = simulationRun,
                        deliberationState = deliberationState,
                        onRefreshDeliberation = { deliberationRefreshKey += 1 },
                        onProfileSave = { user, medical ->
                            val payload = user.toPatientProfilePayload(medical)
                            PatientProfilePersistence.saveDraft(payload)
                            userProfile = user
                            medicalProfile = medical
                            coroutineScope.launch {
                                if (PatientProfileApi.sync(payload)) {
                                    PatientProfilePersistence.clearDraft()
                                    syncMessage = null
                                } else {
                                    syncMessage = "Saved locally. Sync will retry."
                                }
                            }
                        },
                        onRoute = { route = it },
                    ) {
                        simulationRun = true
                        route = AppRoute.Simulation
                    }
                }
                CenteredChrome {
                    BottomNavigation(route = route, onRouteSelected = { route = it })
                }
            }
        }
    }
}

@Composable
private fun RouteContent(
    userProfile: UserProfile?,
    medicalProfile: MedicalProfile?,
    vitals: Vitals,
    route: AppRoute,
    simulationRun: Boolean,
    deliberationState: AgentDeliberationState,
    onRefreshDeliberation: () -> Unit,
    onProfileSave: (UserProfile, MedicalProfile) -> Unit,
    onRoute: (AppRoute) -> Unit,
    onRunSimulation: () -> Unit,
) {
    val user = userProfile
    val medical = medicalProfile ?: MedicalProfile(emptyList(), emptyList(), emptyList())
    val simulation: SimulationResult = SimulationEngine.run(vitals)
    if (user == null) {
        CenteredContent {
            Text("Patient profile is unavailable.", color = HealthColors.TextSecondary, style = MaterialTheme.typography.bodyLarge)
        }
        return
    }
    when (route) {
        AppRoute.Home -> DashboardScreen(
            user = user,
            vitals = vitals,
            result = simulation,
            onRunSimulation = onRunSimulation,
            onRecommendedAction = { onRoute(AppRoute.RecommendedAction) },
            onShare = { onRoute(AppRoute.ShareWithCaregiver) },
            modifier = Modifier.fillMaxSize(),
        )
        AppRoute.Agents -> AgentsScreen(
            agents = DisplayContent.agents,
            user = user,
            medical = medical,
            vitals = vitals,
            deliberationState = deliberationState,
            onRefreshDeliberation = onRefreshDeliberation,
            onConversation = { onRoute(AppRoute.Conversation) },
            modifier = Modifier.fillMaxSize(),
        )
        AppRoute.History -> HistoryScreen(DisplayContent.history, Modifier.fillMaxSize())
        AppRoute.Profile -> ProfileScreen(user, medical, onSave = onProfileSave, modifier = Modifier.fillMaxSize())
        AppRoute.Simulation -> SimulationScreen(user, simulationRun, simulation, onRunSimulation, Modifier.fillMaxSize())
        AppRoute.Conversation -> AgentConversationScreen(DisplayContent.conversation, simulation.recommendation, Modifier.fillMaxSize())
        AppRoute.RecommendedAction -> RecommendedActionScreen(simulation.recommendation, Modifier.fillMaxSize())
        AppRoute.ShareWithCaregiver -> ShareWithCaregiverScreen(user, medical, vitals, simulation, Modifier.fillMaxSize())
    }
}

@Composable
private fun CenteredContent(
    modifier: Modifier = Modifier,
    wide: Boolean = false,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = HealthSpacing.Sm, vertical = HealthSpacing.Md),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = if (wide) HealthSpacing.DesktopContentMaxWidth else HealthSpacing.ContentMaxWidth)
                .fillMaxWidth(),
        ) {
            content()
        }
    }
}

@Composable
private fun CenteredChrome(
    wide: Boolean = false,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(HealthColors.Background),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = if (wide) HealthSpacing.DesktopContentMaxWidth else HealthSpacing.ContentMaxWidth)
                .fillMaxWidth(),
        ) {
            content()
        }
    }
}

@Composable
private fun DesktopTopNavigation(
    route: AppRoute,
    onRouteSelected: (AppRoute) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(HealthColors.Surface, HealthShapes.Pill)
            .padding(HealthSpacing.Xs),
        horizontalArrangement = Arrangement.spacedBy(HealthSpacing.Xs),
    ) {
        primaryRoutes.forEach {
            val selected = route == it
            val color = if (selected) HealthColors.Ink else HealthColors.MutedBlue
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clip(HealthShapes.Pill)
                    .pointerHoverIcon(PointerIcon.Hand)
                    .clickable { onRouteSelected(it) },
                shape = HealthShapes.Pill,
                color = if (selected) HealthColors.SurfaceSubtle else HealthColors.Surface,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = HealthSpacing.Sm, vertical = HealthSpacing.Xs),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    RouteIcon(route = it, color = color)
                    Text(
                        text = it.label,
                        modifier = Modifier.padding(start = HealthSpacing.Xs),
                        color = color,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomNavigation(
    route: AppRoute,
    onRouteSelected: (AppRoute) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(HealthColors.Surface)
            .padding(horizontal = HealthSpacing.Xs, vertical = HealthSpacing.Xs),
        horizontalArrangement = Arrangement.spacedBy(HealthSpacing.Xs),
    ) {
        primaryRoutes.forEach {
            NavItem(
                label = it.label,
                selected = route == it,
                onClick = { onRouteSelected(it) },
                modifier = Modifier.weight(1f),
                compact = true,
            )
        }
    }
}

@Composable
private fun NavItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val background = if (selected) HealthColors.SurfaceSubtle else HealthColors.Surface
    val color = if (selected) HealthColors.Ink else HealthColors.MutedBlue
    val horizontalPadding = if (compact) HealthSpacing.Xs / 2 else HealthSpacing.Sm
    val verticalPadding = if (compact) HealthSpacing.Xs else HealthSpacing.Xs
    Box(
        modifier = modifier
            .clip(HealthShapes.Pill)
            .background(background, HealthShapes.Pill)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(onClick = onClick)
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(HealthSpacing.Xs)) {
            RouteIcon(route = primaryRoutes.first { it.label == label }, color = color)
            Text(
                label.uppercase(),
                color = color,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Clip,
            )
        }
    }
}

private val primaryRoutes = listOf(
    AppRoute.Home,
    AppRoute.Agents,
    AppRoute.History,
    AppRoute.Profile,
)

@Composable
private fun RouteIcon(
    route: AppRoute,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(HealthSpacing.NavIcon)) {
        val stroke = Stroke(width = HealthSpacing.Stroke.toPx() * 2.0f, cap = StrokeCap.Round)
        val w = size.width
        val h = size.height
        when (route) {
            AppRoute.Home -> {
                drawLine(color, Offset(w * 0.18f, h * 0.46f), Offset(w * 0.50f, h * 0.18f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(color, Offset(w * 0.50f, h * 0.18f), Offset(w * 0.82f, h * 0.46f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawRect(color, topLeft = Offset(w * 0.28f, h * 0.46f), size = Size(w * 0.44f, h * 0.36f), style = stroke)
            }
            AppRoute.Agents -> {
                listOf(0.28f, 0.50f, 0.72f).forEachIndexed { index, x ->
                    val top = listOf(0.50f, 0.30f, 0.42f)[index]
                    drawLine(color, Offset(w * x, h * 0.80f), Offset(w * x, h * top), strokeWidth = stroke.width, cap = StrokeCap.Round)
                    drawCircle(color, radius = w * 0.05f, center = Offset(w * x, h * top))
                }
            }
            AppRoute.History -> {
                drawRect(color, topLeft = Offset(w * 0.26f, h * 0.18f), size = Size(w * 0.48f, h * 0.64f), style = stroke)
                drawLine(color, Offset(w * 0.36f, h * 0.36f), Offset(w * 0.64f, h * 0.36f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(color, Offset(w * 0.36f, h * 0.52f), Offset(w * 0.64f, h * 0.52f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(color, Offset(w * 0.36f, h * 0.68f), Offset(w * 0.54f, h * 0.68f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            }
            AppRoute.Profile -> {
                drawCircle(color, radius = w * 0.13f, center = Offset(w * 0.50f, h * 0.32f), style = stroke)
                drawArc(color, startAngle = 205f, sweepAngle = 130f, useCenter = false, topLeft = Offset(w * 0.26f, h * 0.48f), size = Size(w * 0.48f, h * 0.42f), style = stroke)
            }
            else -> {
                drawCircle(color, radius = w * 0.28f, center = Offset(w * 0.50f, h * 0.50f), style = stroke)
            }
        }
    }
}
