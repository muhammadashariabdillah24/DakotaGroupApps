package com.dakotagroupstaff.data.remote.retrofit

import com.google.gson.Gson
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
import java.lang.reflect.Type

/**
 * String Response Converter Factory
 * 
 * Purpose:
 * Backend sends responses as JSON.stringify() which creates a double-stringified JSON
 * This converter handles the conversion process:
 * 1. Receive stringified JSON from backend
 * 2. Parse the outer string layer
 * 3. Parse the actual JSON object using Gson
 * 
 * This is equivalent to JSON.parse() in JavaScript
 */
class StringResponseConverterFactory(
    private val gson: Gson
) : Converter.Factory() {
    
    override fun responseBodyConverter(
        type: Type,
        annotations: Array<out Annotation>,
        retrofit: Retrofit
    ): Converter<ResponseBody, *> {
        return StringResponseConverter(gson, type)
    }
}

/**
 * String Response Converter
 * Handles the actual conversion of response body to target type
 */
class StringResponseConverter(
    private val gson: Gson,
    private val type: Type
) : Converter<ResponseBody, Any> {
    
    override fun convert(value: ResponseBody): Any? {
        val responseString = value.string()
        
        // Backend sends JSON.stringify(response), so we need to:
        // 1. Parse the outer string layer (remove quotes) if double-stringified
        // 2. Parse the inner JSON using Gson
        return try {
            // If response starts with a quote, it's double-stringified
            if (responseString.startsWith("\"")) {
                // Remove outer quotes and unescape
                val unescaped = gson.fromJson(responseString, String::class.java)
                // Parse the actual JSON
                gson.fromJson(unescaped, type)
            } else {
                // Already proper JSON, parse directly
                gson.fromJson(responseString, type)
            }
        } catch (e: Exception) {
            throw IllegalStateException(
                "Failed to parse response: ${responseString.take(100)}...", 
                e
            )
        }
    }
}
