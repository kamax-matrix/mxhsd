/*
 * mxhsd - Corporate Matrix Homeserver
 * Copyright (C) 2018 Maxime Dor
 *
 * https://www.kamax.io/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.kamax.mxhsd.spring.federation.controller.v1.transaction;

import com.google.gson.JsonObject;
import io.kamax.mxhsd.GsonUtil;
import io.kamax.mxhsd.api.IHomeServer;
import io.kamax.mxhsd.core.event.Event;
import io.kamax.mxhsd.core.federation.Transaction;
import io.kamax.mxhsd.spring.common.controller.EmptyJsonResponse;
import io.kamax.mxhsd.spring.common.controller.JsonController;
import io.kamax.mxhsd.spring.common.service.HomeserverService;
import io.kamax.mxhsd.spring.federation.controller.v1.FederationAPIv1;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.stream.Collectors;

@RestController
@RequestMapping(path = FederationAPIv1.Base, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class TransactionController extends JsonController {

    private final Logger logger = LoggerFactory.getLogger(TransactionController.class);

    private IHomeServer hs;

    @Autowired
    public TransactionController(HomeserverService svc) {
        this.hs = svc.get();
    }

    @PutMapping("/send/{transactionId:.+}/")
    public String makeJoin(
            HttpServletRequest req,
            @PathVariable String transactionId
    ) {
        log(logger, req);
        JsonObject tRaw = getJsonObject(req);
        TransactionJson json = GsonUtil.get().fromJson(tRaw, TransactionJson.class);
        hs.getServerSession("").push(new Transaction(
                transactionId,
                json.getOrigin(),
                Instant.ofEpochMilli(json.getOriginServerTs()),
                json.getPdus().stream().map(Event::new).collect(Collectors.toList())));

        return EmptyJsonResponse.stringify();
    }

}
