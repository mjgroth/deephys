@file:UnnamedPackageIsOk


import kotlinx.browser.document
import kotlinx.browser.window
import matt.css.Color.black
import matt.css.Color.white
import matt.kjs.css.sty
import matt.kjs.html.elements.body.wrapped
import matt.model.code.ok.UnnamedPackageIsOk
import matt.nn.deephys.web.DeephysWebMain
import org.w3c.dom.COMPLETE
import org.w3c.dom.DocumentReadyState
import org.w3c.dom.HTMLBodyElement
import org.w3c.dom.Text

private object Context: DeephysWebMain

fun main() {
  whenLoaded {
	document.body!!.appendChild(Text("hello deephys from kotlin JS"))
	(document.body!! as HTMLBodyElement).wrapped().sty {
	  background = black
	  color = white
	}
  }
}

private var windowDidLoad = false

/*https://stackoverflow.com/questions/13364613/how-to-know-if-window-load-event-was-fired-already*/
fun whenLoaded(op: ()->Unit) {
  if (windowDidLoad) {
	op()
  } else {
	val timeout: Int = 200
	var called = false


	window.addEventListener("load", {
	  if (!called) {
		called = true
		windowDidLoad = true
		op()
	  }
	})

	window.setTimeout({
	  if (!called && document.readyState == DocumentReadyState.COMPLETE) {
		called = true
		windowDidLoad = true
		op()
	  }
	}, timeout)
  }


}