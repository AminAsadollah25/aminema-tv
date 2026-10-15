package com.amin.tvos.data

import java.io.File
import java.net.URI

/** Owner-authorized retirement. FarsiPlex is NOT ParsiFlix. No cookie/storage reset. */
object RetiredParsiFlix {
    fun service(id:String)=id=="parsiflix"
    fun page(url:String)=runCatching {
        val host=URI(url).host.orEmpty().lowercase()
        host=="parsiflix.com" || host.endsWith(".parsiflix.com")
    }.getOrDefault(false)
    fun backup(file:File){
        val backup=File(file.parentFile,file.name+".before-farsiland")
        if(file.exists() && !backup.exists())file.copyTo(backup,overwrite=false)
    }
}
