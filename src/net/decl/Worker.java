/*
 * Copyright (c) 2017. Markus Monz
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.decl;

import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class Worker<T> implements Runnable {
    private static final Logger log = Logger.getLogger(Worker.class.getName());
    protected T data;

    public Worker(T data) {
        this.data = data;
    }

    abstract public void serve();

    @Override
    public void run() {
        try {
            serve();
        } catch (Exception e) {
            log.log(Level.SEVERE, "Ooops!", e);
        }
    }
}
