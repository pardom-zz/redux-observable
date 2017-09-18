package redux.observable

import io.reactivex.Observable
import redux.api.Store

/*
 * Copyright (C) 2016 Michael Pardo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

interface Epic<S : Any> {

    fun map(actions: Observable<out Any>, store: Store<S>): Observable<out Any>

    companion object {

        operator fun <S : Any> invoke(f: (Observable<out Any>, Store<S>) -> Observable<out Any>) = object : Epic<S> {
            override fun map(actions: Observable<out Any>, store: Store<S>) = f(actions, store)
        }

    }

}

fun <S : Any> combineEpics(vararg epics: Epic<S>): Epic<S> {
    return object : Epic<S> {
        override fun map(actions: Observable<out Any>, store: Store<S>): Observable<out Any> {
            return Observable.merge(epics.map { it.map(actions, store) })
        }
    }
}
