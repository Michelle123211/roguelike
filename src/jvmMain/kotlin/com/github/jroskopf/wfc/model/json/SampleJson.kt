package com.github.jroskopf.wfc.model.json

import com.google.gson.annotations.SerializedName

data class SampleJson(
        @SerializedName("samples")
        val samples: Samples?
)