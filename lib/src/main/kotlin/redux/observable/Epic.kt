package redux.observable

import redux.Store
import rx.Observable

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

interface Epic<S : Any, A : Any> {

	fun map(actions: Observable<A>, store: Store<S, A>): Observable<A>

	companion object {

		fun <S : Any, A : Any> combine(vararg epics: Epic<S, A>): Epic<S, A> {
			return object : Epic<S, A> {
				override fun map(actions: Observable<A>, store: Store<S, A>): Observable<A> {
					return Observable.merge(epics.map { it.map(actions, store) })
				}
			}
		}

	}

}
