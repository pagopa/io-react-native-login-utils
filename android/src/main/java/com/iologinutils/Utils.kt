package com.iologinutils

import com.facebook.react.bridge.WritableArray
import com.facebook.react.bridge.WritableNativeArray
import org.json.JSONObject

fun generateErrorObject(error:String, responseCode: Int?, url:String?, parameters: List<String>?): String {
  val json = JSONObject()
  json.put("error", error)
  if(responseCode != null){
    json.put("statusCode", responseCode)
  }
  if(url != null) {
    json.put("url", url)
    if (parameters != null) {
      val writableArray: WritableArray = WritableNativeArray()
      for (str in parameters) {
        writableArray.pushString(str)
      }
      json.put("parameters",writableArray)
    }
  }
  return json.toString()
}
