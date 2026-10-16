package app.simplifymy.browser

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory

inline fun <reified VM : ViewModel> componentsFactory(crossinline create: (Components) -> VM) = viewModelFactory {
    initializer { create((this[APPLICATION_KEY] as BrowserApp).components) }
}
