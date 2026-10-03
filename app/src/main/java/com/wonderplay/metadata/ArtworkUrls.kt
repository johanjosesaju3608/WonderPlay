package com.wonderplay.metadata

/** Google music covers advertise a tiny search size, but the same asset supports larger requests. */
object ArtworkUrls {
    fun forSize(url:String,size:Int):String {
        val host=runCatching { java.net.URI(url).host.orEmpty() }.getOrDefault("")
        if(!(host.endsWith(".googleusercontent.com") || host.endsWith(".ggpht.com"))) return url
        val pixels=size.coerceIn(96,1024)
        return url.replace(Regex("w\\d+-h\\d+"),"w$pixels-h$pixels")
    }
}
