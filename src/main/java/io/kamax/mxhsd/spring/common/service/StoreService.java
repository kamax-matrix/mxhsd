/*
 * mxhsd - Corporate Matrix Homeserver
 * Copyright (C) 2018 Kamax Sarl
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

package io.kamax.mxhsd.spring.common.service;

import io.kamax.mxhsd.api.store.IStore;
import io.kamax.mxhsd.core.store.InMemoryStore;
import io.kamax.mxhsd.core.store.PostgreSqlStore;
import io.kamax.mxhsd.spring.common.config.StorageConfig;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class StoreService {

    @Bean
    public IStore get(StorageConfig cfg) {
        if (StringUtils.equals("postgresql", cfg.getType())) {
            return new PostgreSqlStore(cfg);
        }

        if (StringUtils.equals("memory", cfg.getType())) {
            return new InMemoryStore();
        }

        throw new RuntimeException("Invalid storage type: " + cfg.getType());
    }

}
