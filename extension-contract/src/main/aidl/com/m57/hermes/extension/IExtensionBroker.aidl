package com.m57.hermes.extension;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import com.m57.hermes.extension.IExtensionCallback;
interface IExtensionBroker {
    Bundle negotiate(int major, int minor, int capabilities) = 0;
    Bundle openSession(int major, int capabilities, IExtensionCallback callback) = 1;
    Bundle stageInput(String grant, String epoch, String operationId, String text, in ParcelFileDescriptor jpeg) = 2;
    Bundle observe(String grant, String epoch) = 3;
    Bundle getOperationStatus(String grant, String epoch, String operationId) = 4;
    void closeSession(String grant, String epoch) = 5;
}
