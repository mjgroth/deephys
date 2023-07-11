package matt.nn.deephys.calc.act

import matt.lang.require.requireEquals
import matt.lang.require.requireOne
import matt.math.jmath.sigFigs
import matt.model.data.mathable.DoubleWrapper
import matt.model.data.mathable.FloatWrapper
import matt.model.data.mathable.NumberWrapper
import matt.nn.deephys.calc.act.ActivationRatio.Companion.ACT_RATIO_SYMBOL
import matt.nn.deephys.calc.act.RawActivation.Companion.RAW_ACT_SYMBOL

sealed interface Activation<N : Number, T : Activation<N, T>> : NumberWrapper<T>, Comparable<T> {
    val value: N
    val formatted: String
    val extraInfo: String?
}


sealed interface ActivationFloat32<T : ActivationFloat32<T>> : Activation<Float, T>, FloatWrapper<T> {
    override val value: Float
    override val asFloat: Float
        get() = value

}

sealed interface ActivationFloat64<T : ActivationFloat64<T>> : Activation<Double, T>, DoubleWrapper<T> {
    override val value: Double
    override val asDouble: Double
        get() = value

}


sealed interface AlwaysOneActivation<N : Number, T : AlwaysOneActivation<N, T>> : Activation<N, T> {
    override val extraInfo get() = null /*"In this case, the activation is always exactly 1"*/
}

object AlwaysOneActivationFloat32 : AlwaysOneActivation<Float, AlwaysOneActivationFloat32>,
    ActivationFloat32<AlwaysOneActivationFloat32> {
    override fun fromFloat(d: Float): AlwaysOneActivationFloat32 {
        requireOne(d)
        return AlwaysOneActivationFloat32
    }

    override val value = 1f
    override val formatted: String get() = "max: 100%"

}

object AlwaysOneActivationFloat64 : AlwaysOneActivation<Double, AlwaysOneActivationFloat64>,
    ActivationFloat64<AlwaysOneActivationFloat64> {
    override fun fromDouble(d: Double): AlwaysOneActivationFloat64 {
        requireOne(d)
        return AlwaysOneActivationFloat64
    }

    override val value = 1.0
    override val formatted: String get() = ""

}


sealed interface RawActivation<A : Number, T : RawActivation<A, T>> : Activation<A, T> {


    companion object {
        const val RAW_ACT_SYMBOL = "max"
    }

    override val extraInfo: String?
        get() = null

}

@JvmInline
value class RawActivationFloat32(override val value: Float) : RawActivation<Float, RawActivationFloat32>,
    ActivationFloat32<RawActivationFloat32> {


    override val formatted get() = "$RAW_ACT_SYMBOL: ${value.sigFigs(3)}"
    override fun fromFloat(d: Float): RawActivationFloat32 {
        return RawActivationFloat32(d)
    }


}

@JvmInline
value class RawActivationFloat64(override val value: Double) : RawActivation<Double, RawActivationFloat64>,
    ActivationFloat64<RawActivationFloat64> {


    override val formatted get() = "$RAW_ACT_SYMBOL: ${value.sigFigs(3)}"
    override fun fromDouble(d: Double): RawActivationFloat64 {
        return RawActivationFloat64(d)
    }


}

/*
sealed interface NormalActivation<A: Number, T: NormalActivation<A, T>>: Activation<A, T> {


  companion object {
	const val NORMALIZED_ACT_SYMBOL = "Ŷ"
  }

  override val extraInfo: String?
	get() = null

}*/

/*
@JvmInline
value class NormalActivationFloat32(override val value: Float): NormalActivation<Float, NormalActivationFloat32>,
																ActivationFloat32<NormalActivationFloat32> {


  override val formatted get() = " $NORMALIZED_ACT_SYMBOL=${value.sigFigs(3)}"
  override fun plus(m: NormalActivationFloat32): NormalActivationFloat32 {
	return NormalActivationFloat32(value + m.value)
  }

  override fun fromFloat(d: Float): NormalActivationFloat32 {
	return NormalActivationFloat32(d)
  }

  override fun div(n: Number): NormalActivationFloat32 {
	return NormalActivationFloat32(value/n.toFloat())
  }


}
*/

/*

@JvmInline
value class NormalActivationFloat64(override val value: Double): NormalActivation<Double, NormalActivationFloat64>,
																 ActivationFloat64<NormalActivationFloat64> {


  override val formatted get() = " $NORMALIZED_ACT_SYMBOL=${value.sigFigs(3)}"
  override fun plus(m: NormalActivationFloat64): NormalActivationFloat64 {
	return NormalActivationFloat64(value + m.value)
  }

  override fun fromDouble(d: Double): NormalActivationFloat64 {
	return NormalActivationFloat64(d)
  }

  override fun div(n: Number): NormalActivationFloat64 {
	return NormalActivationFloat64(value/n.toDouble())
  }
}
*/



sealed interface ActivationRatio<A : Number, T : ActivationRatio<A, T>> : Activation<A, T> {


    companion object {
        const val ACT_RATIO_SYMBOL = "%"
    }

    override val extraInfo: String?
        get() = if (isNaN || isInfinite) "The activation percentage will be infinite or NaN if the corresponding activation in the InD network is absolute 0. If the activation was extremely small but non-zero in python and the current test is float32, it may have become zero when the precision was lost. You may try recreating the .test file with float64 precision to see if that fixes it." else null

}

@JvmInline
value class ActivationRatioFloat32(override val value: Float) : ActivationRatio<Float, ActivationRatioFloat32>,
    ActivationFloat32<ActivationRatioFloat32> {


    override val formatted get() = "max: ${(value * 100).sigFigs(3)}${ACT_RATIO_SYMBOL}"
    override fun plus(m: ActivationRatioFloat32): ActivationRatioFloat32 {
        return ActivationRatioFloat32(value + m.value)
    }

    override fun fromFloat(d: Float): ActivationRatioFloat32 {
        return ActivationRatioFloat32(d)
    }

    override fun div(n: Number): ActivationRatioFloat32 {
        return ActivationRatioFloat32(value / n.toFloat())
    }
}


@JvmInline
value class ActivationRatioFloat64(override val value: Double) : ActivationRatio<Double, ActivationRatioFloat64>,
    ActivationFloat64<ActivationRatioFloat64> {


    override val formatted get() = "max: ${(value * 100).sigFigs(3)}$ACT_RATIO_SYMBOL"
    override fun plus(m: ActivationRatioFloat64): ActivationRatioFloat64 {
        return ActivationRatioFloat64(value + m.value)
    }

    override fun fromDouble(d: Double): ActivationRatioFloat64 {
        return ActivationRatioFloat64(d)
    }

    override fun div(n: Number): ActivationRatioFloat64 {
        return ActivationRatioFloat64(value / n.toDouble())
    }
}




