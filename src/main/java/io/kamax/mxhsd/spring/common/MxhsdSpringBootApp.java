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

package io.kamax.mxhsd.spring.common;

import io.kamax.mxhsd.spring.client.MxhsdClientApp;
import io.kamax.mxhsd.spring.common.controller.DefaultExceptionHandler;
import io.kamax.mxhsd.spring.federation.MxhsdFederationApp;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
public class MxhsdSpringBootApp {

    public static void main(String[] args) {
        try {
            SpringApplicationBuilder builder = new SpringApplicationBuilder(MxhsdSpringBootApp.class);
            builder.web(false);

            builder.child(MxhsdClientApp.class).sources(DefaultExceptionHandler.class).run(args);
            builder.child(MxhsdFederationApp.class).sources(DefaultExceptionHandler.class).run(args);
        } catch (Throwable t) {
            System.exit(1);
        }
    }

}
