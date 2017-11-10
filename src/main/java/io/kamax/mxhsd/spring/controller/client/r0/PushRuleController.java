/*
 * mxhsd - Corporate Matrix Homeserver
 * Copyright (C) 2017 Maxime Dor
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

package io.kamax.mxhsd.spring.controller.client.r0;

import com.google.gson.Gson;
import io.kamax.mxhsd.GsonUtil;
import io.kamax.mxhsd.spring.controller.JsonController;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

@RestController
@RequestMapping(path = ClientAPIr0.Base + "/pushrules", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class PushRuleController extends JsonController {

    private Gson gson = GsonUtil.build();

    // FIXME clean up
    @RequestMapping(method = GET, path = "/")
    public String list(HttpServletRequest req) {
        log(req);

        // Straight up from synapse until we support this - Seems like default rules
        // Without this, Riot just fails to handle new room creation.
        String json = "{\"device\":{},\"global\":{\"content\":[{\"default\":true,\"pattern\":\"z\",\"enabled\":true," +
                "\"rule_id\":\".m.rule.contains_user_name\",\"actions\":[\"notify\",{\"set_tweak\":\"sound\"," +
                "\"value\":\"default\"},{\"set_tweak\":\"highlight\"}]}],\"override\":[{\"default\":true," +
                "\"enabled\":false,\"conditions\":[],\"rule_id\":\".m.rule.master\",\"actions\":[\"dont_notify\"]}," +
                "{\"default\":true,\"enabled\":true,\"conditions\":[{\"pattern\":\"m.notice\",\"kind\":" +
                "\"event_match\",\"key\":\"content.msgtype\"}],\"rule_id\":\".m.rule.suppress_notices\",\"actions\":" +
                "[\"dont_notify\"]},{\"default\":true,\"enabled\":true,\"conditions\":[{\"pattern\":\"m.room.member\"," +
                "\"kind\":\"event_match\",\"key\":\"type\"},{\"pattern\":\"invite\",\"kind\":\"event_match\"," +
                "\"key\":\"content.membership\"},{\"pattern\":\"@z:localhost.kamax.io\",\"kind\":\"event_match\"," +
                "\"key\":\"state_key\"}],\"rule_id\":\".m.rule.invite_for_me\",\"actions\":[\"notify\"," +
                "{\"set_tweak\":\"sound\",\"value\":\"default\"},{\"set_tweak\":\"highlight\",\"value\":false}]}," +
                "{\"default\":true,\"enabled\":true,\"conditions\":[{\"pattern\":\"m.room.member\"," +
                "\"kind\":\"event_match\",\"key\":\"type\"}],\"rule_id\":\".m.rule.member_event\",\"actions\":" +
                "[\"dont_notify\"]},{\"default\":true,\"enabled\":true,\"conditions\":[{\"kind\":" +
                "\"contains_display_name\"}],\"rule_id\":\".m.rule.contains_display_name\",\"actions\":" +
                "[\"notify\",{\"set_tweak\":\"sound\",\"value\":\"default\"},{\"set_tweak\":\"highlight\"}]}," +
                "{\"default\":true,\"enabled\":true,\"conditions\":[{\"pattern\":\"@room\",\"kind\":\"event_match\"," +
                "\"key\":\"content.body\"},{\"kind\":\"sender_notification_permission\",\"key\":\"room\"}]," +
                "\"rule_id\":\".m.rule.roomnotif\",\"actions\":[\"notify\",{\"set_tweak\":\"highlight\"," +
                "\"value\":true}]}],\"sender\":[],\"room\":[],\"underride\":[{\"default\":true,\"enabled\":true," +
                "\"conditions\":[{\"pattern\":\"m.call.invite\",\"kind\":\"event_match\",\"key\":\"type\"}]," +
                "\"rule_id\":\".m.rule.call\",\"actions\":[\"notify\",{\"set_tweak\":\"sound\",\"value\":\"ring\"}," +
                "{\"set_tweak\":\"highlight\",\"value\":false}]},{\"default\":true,\"enabled\":true,\"conditions\":" +
                "[{\"kind\":\"room_member_count\",\"is\":\"2\"},{\"pattern\":\"m.room.message\",\"kind\":" +
                "\"event_match\",\"key\":\"type\"}],\"rule_id\":\".m.rule.room_one_to_one\",\"actions\":[\"notify\"," +
                "{\"set_tweak\":\"sound\",\"value\":\"default\"},{\"set_tweak\":\"highlight\",\"value\":false}]}," +
                "{\"default\":true,\"enabled\":true,\"conditions\":[{\"kind\":\"room_member_count\",\"is\":\"2\"}," +
                "{\"pattern\":\"m.room.encrypted\",\"kind\":\"event_match\",\"key\":\"type\"}],\"rule_id\":" +
                "\".m.rule.encrypted_room_one_to_one\",\"actions\":[\"notify\",{\"set_tweak\":\"sound\"," +
                "\"value\":\"default\"},{\"set_tweak\":\"highlight\",\"value\":false}]},{\"default\":true," +
                "\"enabled\":true,\"conditions\":[{\"pattern\":\"m.room.message\",\"kind\":\"event_match\"," +
                "\"key\":\"type\"}],\"rule_id\":\".m.rule.message\",\"actions\":[\"notify\",{\"set_tweak\":" +
                "\"highlight\",\"value\":false}]},{\"default\":true,\"enabled\":true,\"conditions\":[{\"pattern\":" +
                "\"m.room.encrypted\",\"kind\":\"event_match\",\"key\":\"type\"}],\"rule_id\":\".m.rule.encrypted\"," +
                "\"actions\":[\"notify\",{\"set_tweak\":\"highlight\",\"value\":false}]}]}}";

        return json;
    }

}
