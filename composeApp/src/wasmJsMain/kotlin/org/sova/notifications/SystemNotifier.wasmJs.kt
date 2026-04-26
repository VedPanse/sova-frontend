package org.sova.notifications

import kotlinx.coroutines.await
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsString
import kotlin.js.Promise

@OptIn(ExperimentalWasmJsInterop::class)
actual object SystemNotifier {
    actual suspend fun show(title: String, message: String) {
        runCatching {
            showNotification(title.toJsString(), message.toJsString()).await<JsString>()
        }.onFailure {
            println("Sova notification failed: ${it.message ?: it::class.simpleName}")
        }
    }
}

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun(
    """
    (title, message) => {
        if (typeof Notification === 'undefined') {
            return Promise.resolve('unsupported');
        }
        const fire = () => {
            new Notification(title, { body: message });
            return 'shown';
        };
        if (Notification.permission === 'granted') {
            return Promise.resolve(fire());
        }
        return Notification.requestPermission().then((permission) => {
            if (permission === 'granted') return fire();
            return 'denied';
        });
    }
    """,
)
private external fun showNotification(title: JsString, message: JsString): Promise<JsString>
