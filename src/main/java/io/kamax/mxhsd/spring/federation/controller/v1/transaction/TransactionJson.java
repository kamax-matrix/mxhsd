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

import java.util.List;

public class TransactionJson {

    private String origin;
    private long originServerTs;
    private List<JsonObject> pdus;

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public long getOriginServerTs() {
        return originServerTs;
    }

    public void setOriginServerTs(long originalServerTs) {
        this.originServerTs = originalServerTs;
    }

    public List<JsonObject> getPdus() {
        return pdus;
    }

    public void setPdus(List<JsonObject> pdus) {
        this.pdus = pdus;
    }

}
