package matt.nn.deephy.pref

import java.util.prefs.Preferences
import kotlin.reflect.KProperty

class Pref(private val defaultValue: String? = null) {

  companion object {
	private val prefs: Preferences = Preferences.userRoot().node("sinhalab.deephy")
  }

  operator fun provideDelegate(
	thisRef: Any?,
	prop: KProperty<*>
  ): Pref {
	return this
  }

  operator fun getValue(
	thisRef: Any?,
	property: KProperty<*>
  ) = prefs.get(property.name, defaultValue)


  operator fun setValue(
	thisRef: Any?,
	property: KProperty<*>,
	value: String?
  ) {
	if (value == null) {
	  prefs.remove(property.name)
	} else prefs.put(property.name, value)
  }
}
