package redux.observable

import org.jetbrains.spek.api.Spek
import redux.Middleware
import redux.Reducer
import redux.Store
import redux.observable.helpers.ActionCreators.Action
import redux.observable.helpers.ActionCreators.Action.Fire1
import redux.observable.helpers.ActionCreators.Action.Fire2
import redux.observable.helpers.ActionCreators.action1
import redux.observable.helpers.ActionCreators.action2
import redux.observable.helpers.ActionCreators.fire1
import redux.observable.helpers.ActionCreators.fire2
import rx.Observable
import kotlin.test.expect

/*
 * Copyright (C) 2016 Michael Pardo
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
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

class EpicMiddlewareTest : Spek({

	describe("EpicMiddlware") {

		describe("create") {

			it("should accept a epic argument that wires up a stream of actions to a stream of actions") {
				val reducer = object : Reducer<List<Action>, Action> {
					override fun reduce(state: List<Action>, action: Action): List<Action> {
						return state + action
					}
				}

				val epic = object : Epic<List<Action>, Action> {
					override fun map(
							actions: Observable<Action>,
							store: Store<List<Action>, Action>): Observable<Action> {

						return Observable.merge(
								actions.ofType(Fire1::class.java).map { action1() },
								actions.ofType(Fire2::class.java).map { action2() }
						)

					}
				}

				val middleware = EpicMiddleware.create(epic)
				val store = Store.create(reducer, emptyList(), Middleware.apply(middleware))

				store.dispatch(fire1())
				store.dispatch(fire2())

				expect(store.getState()) {
					listOf(
							fire1(),
							action1(),
							fire2(),
							action2()
					)
				}
			}

			// TODO: port more tests

		}

	}

})
