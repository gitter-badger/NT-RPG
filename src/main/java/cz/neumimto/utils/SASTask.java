/*    
 *     Copyright (c) 2015, NeumimTo https://github.com/NeumimTo
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *     
 */

package cz.neumimto.utils;


import org.spongepowered.api.Game;
import org.spongepowered.api.service.scheduler.TaskBuilder;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Created by NeumimTo on 17.7.2015.
 */
public class SASTask<T, U> {
    Function<T, U> function;
    Consumer<U> consumer;

    public SASTask async(Function<T, U> funct) {
        this.function = funct;
        return this;
    }

    public SASTask sync(Consumer<U> consumer) {
        this.consumer = consumer;
        return this;
    }

    public void start(T t, Game game, Object plugin) {
        TaskBuilder taskBuilder = game.getScheduler().createTaskBuilder();
        taskBuilder.async().execute(() -> {
            U u1 = function.apply(t);
            game.getScheduler().createTaskBuilder().execute(() -> consumer.accept(u1)).submit(plugin);
        }).submit(plugin);

    }

}