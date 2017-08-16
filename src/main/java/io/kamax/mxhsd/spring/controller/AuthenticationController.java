package io.kamax.mxhsd.spring.controller;

import io.kamax.mxhsd.NoJsonException;
import io.kamax.mxhsd.spring.service.HomeserverService;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.springframework.web.bind.annotation.RequestMethod.POST;

@Controller
@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class AuthenticationController {

    @Autowired
    private HomeserverService svc;

    private String getJson(HttpServletRequest req) {
        try {
            String data = IOUtils.toString(req.getInputStream(), StandardCharsets.UTF_8);
            if (StringUtils.isBlank(data)) {
                throw new NoJsonException("Document is empty");
            }
            return data;
        } catch (IOException e) {
            throw new NoJsonException(e);
        }
    }

    @RequestMapping(method = POST, path = APIr0.Base + "/login")
    public String login(HttpServletRequest req) throws IOException {
        return svc.get().login(getJson(req));
    }

    @RequestMapping(method = POST, path = APIr0.Base + "/tokenrefresh")
    public String tokenRefresh() {
        throw new NotImplementedException("tokenrefresh");
    }

    @RequestMapping(method = POST, path = APIr0.Base + "/logout")
    public String logout(@RequestParam("access_token") String accessToken) {
        svc.get().getUserSession(accessToken).logout();

        return EmptyJsonResponse.stringify();
    }

}
