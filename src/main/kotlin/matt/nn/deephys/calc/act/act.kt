package matt.nn.deephys.calc.act

import matt.math.jmath.sigFigs
import matt.model.data.mathable.FloatWrapper

sealed interface Activation<T: Activation<T>>: FloatWrapper<T> {
  val value: Float
  val formatted: String
  override val asFloat: Float
	get() = value
}

@JvmInline
value class RawActivation(override val value: Float): Activation<RawActivation> {

  companion object {
	const val RAW_ACT_SYMBOL = "Y"
  }

  override val formatted get() = " $RAW_ACT_SYMBOL=${value.sigFigs(3)}"
  override fun fromFloat(d: Float): RawActivation {
	return RawActivation(d)
  }


}

@JvmInline
value class NormalActivation(override val value: Float): Activation<NormalActivation> {
  companion object {
	const val NORMALIZED_ACT_SYMBOL = "Ŷ"
  }

  override val formatted get() = " $NORMALIZED_ACT_SYMBOL=${value.sigFigs(3)}"
  override fun plus(m: NormalActivation): NormalActivation {
	return NormalActivation(value + m.value)
  }

  override fun fromFloat(d: Float): NormalActivation {
	return NormalActivation(d)
  }

  override fun div(n: Number): NormalActivation {
	return NormalActivation(value/n.toFloat())
  }
}

@JvmInline
value class ActivationRatio(override val value: Float): Activation<ActivationRatio> {
  companion object {
	const val ACT_RATIO_SYMBOL = "%"
  }

  override val formatted get() = " $ACT_RATIO_SYMBOL=${value.sigFigs(3)}"
  override fun plus(m: ActivationRatio): ActivationRatio {
	return ActivationRatio(value + m.value)
  }

  override fun fromFloat(d: Float): ActivationRatio {
	return ActivationRatio(d)
  }

  override fun div(n: Number): ActivationRatio {
	return ActivationRatio(value/n.toFloat())
  }
}


