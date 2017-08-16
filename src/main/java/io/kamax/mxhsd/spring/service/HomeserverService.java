package io.kamax.mxhsd.spring.service;

import io.kamax.mxhsd.core.IHomeServer;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Service
public class HomeserverService {

    private IHomeServer srv;

    @PostConstruct
    public void postConstruct() {
        // TODO create instance
    }

    public IHomeServer get() {
        return srv;
    }

}
