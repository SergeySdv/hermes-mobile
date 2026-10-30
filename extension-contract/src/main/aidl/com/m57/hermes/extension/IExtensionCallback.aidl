package com.m57.hermes.extension;
import android.os.Bundle;
oneway interface IExtensionCallback {
    void onEvent(in Bundle event) = 0;
}
