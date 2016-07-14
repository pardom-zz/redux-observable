package redux.observable

import redux.Dispatcher
import redux.Middleware
import redux.Store
import rx.subjects.PublishSubject

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

class EpicMiddleware<S : Any, A : Any> : Middleware<S, A> {
	val actions = PublishSubject.create<A>() // TODO: Use RelaySubject to ignore termination events
	val epic: Epic<S, A>

	var subscribed = false

	private constructor(epic: Epic<S, A>) {
		this.epic = epic
	}

	override fun dispatch(store: Store<S, A>, action: A, next: Dispatcher<A>): A {
		if (!subscribed) {
			epic.map(actions.asObservable(), store).subscribe { store.dispatch(it) }
			subscribed = true
		}

		val result = next.dispatch(action)
		actions.onNext(action)
		return result
	}

	companion object {

		fun <S : Any, A : Any> create(epic: Epic<S, A>): EpicMiddleware<S, A> {
			return EpicMiddleware(epic)
		}

	}
}
