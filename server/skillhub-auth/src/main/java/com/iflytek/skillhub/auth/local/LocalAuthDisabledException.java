package com.iflytek.skillhub.auth.local;

import com.iflytek.skillhub.auth.exception.AuthFlowException;
import org.springframework.http.HttpStatus;

public class LocalAuthDisabledException extends AuthFlowException {

    public LocalAuthDisabledException() {
        super(HttpStatus.FORBIDDEN, "error.auth.local.disabled");
    }
}
