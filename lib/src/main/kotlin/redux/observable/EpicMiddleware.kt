package redux.observable

import redux.Dispatcher
import redux.Middleware
import redux.Store
import rx.subjects.BehaviorSubject
import rx.subjects.PublishSubject
import java.util.concurrent.atomic.AtomicBoolean

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

class EpicMiddleware<S : Any> : Middleware<S> {
	private val actions = PublishSubject.create<Any>()
	private val epics: BehaviorSubject<Epic<S>>
	private val subscribed = AtomicBoolean(false)

	private constructor(epic: Epic<S>) {
		epics = BehaviorSubject.create(epic)
	}

	override fun dispatch(store: Store<S>, action: Any, next: Dispatcher): Any {
		if (subscribed.compareAndSet(false, true)) {
			epics.switchMap { it.map(actions, store) }.subscribe { store.dispatch(it) }
		}

		val result = next.dispatch(action)
		actions.onNext(action)
		return result
	}

	fun replaceEpic(epic: Epic<S>) {
		epics.onNext(epic)
	}

	companion object {

		fun <S : Any> create(epic: Epic<S>): EpicMiddleware<S> {
			return EpicMiddleware(epic)
		}

	}
}
