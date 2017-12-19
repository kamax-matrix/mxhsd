package io.kamax.mxhsd.spring.federation.controller.v1.query;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.kamax.mxhsd.api.IHomeServer;
import io.kamax.mxhsd.api.exception.NotFoundException;
import io.kamax.mxhsd.api.room.directory.IRoomAliasLookup;
import io.kamax.mxhsd.spring.common.controller.JsonController;
import io.kamax.mxhsd.spring.common.service.HomeserverService;
import io.kamax.mxhsd.spring.federation.controller.v1.FederationAPIv1;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

@RestController
@RequestMapping(path = FederationAPIv1.Query, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class RoomDirectoryController extends JsonController {

    private IHomeServer hs;

    @Autowired
    public RoomDirectoryController(HomeserverService svc) {
        this.hs = svc.get();
    }

    @RequestMapping(method = GET, path = "/directory")
    public String queryRoomAlias(HttpServletRequest req, @RequestParam("room_alias") String roomAlias) {
        log(req);

        IRoomAliasLookup lookup = hs.getServerSession("").getDirectory().lookup(roomAlias)
                .orElseThrow(() -> new NotFoundException("No room with alias " + roomAlias + " exists"));

        JsonArray servers = new JsonArray();
        lookup.getServers().forEach(servers::add);
        JsonObject body = new JsonObject();
        body.addProperty("room_id", lookup.getId());
        body.add("servers", servers);

        return toJson(body);
    }

}
