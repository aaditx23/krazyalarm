import com.aaditx23.krazyalarm.domain.models.Alarm

data class UiState(
    val isLoading: Boolean = false,
    val alarms: List<Alarm> = emptyList(),
    val errorMessage: String? = null,
    val showSheet: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val isSelectMode: Boolean = false,
    val selectedAlarms: Set<Long> = emptySet(),
    val showDatePicker: Boolean = false
)

sealed class UiEvent {
    data class Error(val message: String) : UiEvent()
    data class Success(val message: String) : UiEvent()
}
