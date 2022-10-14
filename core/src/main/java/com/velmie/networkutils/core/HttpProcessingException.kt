package com.velmie.networkutils.core

import java.io.IOException

class HttpProcessingException(message: String) :
    IOException("Request processing on the server ended with an error:\n $message")