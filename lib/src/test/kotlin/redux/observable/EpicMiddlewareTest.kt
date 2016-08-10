package redux.observable

import org.jetbrains.spek.api.Spek
import redux.Middleware
import redux.Reducer
import redux.Store
import redux.Store.Companion.INIT
import redux.asObservable
import redux.observable.helpers.ActionCreators.Action.Action1
import redux.observable.helpers.ActionCreators.Action.Action2
import redux.observable.helpers.ActionCreators.Action.Action3
import redux.observable.helpers.ActionCreators.Action.Action4
import redux.observable.helpers.ActionCreators.Action.Epic1Generic
import redux.observable.helpers.ActionCreators.Action.Epic2Generic
import redux.observable.helpers.ActionCreators.Action.Fire1
import redux.observable.helpers.ActionCreators.Action.Fire2
import redux.observable.helpers.ActionCreators.Action.Fire3
import redux.observable.helpers.ActionCreators.Action.Fire4
import redux.observable.helpers.ActionCreators.Action.FireGeneric
import rx.Observable
import rx.observers.TestSubscriber
import rx.plugins.RxPluginsPackage
import rx.schedulers.TestScheduler
import java.util.concurrent.TimeUnit.MILLISECONDS
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

	beforeEach { RxPluginsPackage.setUp() }

	describe("EpicMiddleware") {

		describe("create") {

			it("should accept a epic argument that wires up a stream of actions to a stream of actions") {
				val reducer = object : Reducer<List<Any>> {
					override fun reduce(state: List<Any>, action: Any): List<Any> {
						return state + action
					}
				}

				val epic = object : Epic<List<Any>> {
					override fun map(actions: Observable<out Any>, store: Store<List<Any>>): Observable<out Any> {
						return Observable.merge(
								actions.ofType(Fire1::class.java).map { Action1 },
								actions.ofType(Fire2::class.java).map { Action2 }
						)
					}
				}

				val store = Store.create(reducer, emptyList(), Middleware.apply(EpicMiddleware.create(epic)))

				store.dispatch(Fire1)
				store.dispatch(Fire2)

				expect(listOf(
						INIT,
						Fire1,
						Action1,
						Fire2,
						Action2

				)) { store.getState() }
			}

			it("should allow you to replace the root epic with replaceEpic") {
				val reducer = object : Reducer<List<Any>> {
					override fun reduce(state: List<Any>, action: Any): List<Any> {
						return state + action
					}
				}

				val epic1 = object : Epic<List<Any>> {
					override fun map(actions: Observable<out Any>, store: Store<List<Any>>): Observable<out Any> {
						return Observable.merge(
								actions.ofType(Fire1::class.java).map { Action1 },
								actions.ofType(Fire2::class.java).map { Action2 },
								actions.ofType(FireGeneric::class.java).map { Epic1Generic }
						)
					}
				}

				val epic2 = object : Epic<List<Any>> {
					override fun map(
							actions: Observable<out Any>,
							store: Store<List<Any>>): Observable<out Any> {

						// Simulate network requests
						return Observable.merge(
								actions.ofType(Fire3::class.java).map { Action3 },
								actions.ofType(Fire4::class.java).map { Action4 },
								actions.ofType(FireGeneric::class.java).map { Epic2Generic }
						)
					}
				}

				val middleware = EpicMiddleware.create(epic1)

				val store = Store.create(reducer, emptyList(), Middleware.apply(middleware))

				store.dispatch(Fire1)
				store.dispatch(Fire2)
				store.dispatch(FireGeneric)

				middleware.replaceEpic(epic2)

				store.dispatch(Fire3)
				store.dispatch(Fire4)
				store.dispatch(FireGeneric)

				expect(listOf(
						INIT,
						Fire1,
						Action1,
						Fire2,
						Action2,
						FireGeneric,
						Epic1Generic,
						Fire3,
						Action3,
						Fire4,
						Action4,
						FireGeneric,
						Epic2Generic

				)) { store.getState() }
			}

			it("should dispatch actions mapped to other threads") {
				val scheduler = TestScheduler()
				val subscriber = TestSubscriber<List<Any>>()

				val reducer = object : Reducer<List<Any>> {
					override fun reduce(state: List<Any>, action: Any): List<Any> {
						return state + action
					}
				}

				val epic = object : Epic<List<Any>> {
					override fun map(actions: Observable<out Any>, store: Store<List<Any>>): Observable<out Any> {
						// Simulate network requests
						val networkRequest1 = Observable.just(Action1)
								.subscribeOn(scheduler)
								.delay(100L, MILLISECONDS, scheduler)
						val networkRequest2 = Observable.just(Action2)
								.subscribeOn(scheduler)
								.delay(200L, MILLISECONDS, scheduler)

						return Observable.merge(
								actions.ofType(Fire1::class.java).flatMap { networkRequest1 },
								actions.ofType(Fire2::class.java).flatMap { networkRequest2 }
						)
					}
				}

				val store = Store.create(reducer, emptyList(), Middleware.apply(EpicMiddleware.create(epic)))
				store.asObservable().subscribe(subscriber)

				store.dispatch(Fire1)
				store.dispatch(Fire2)

				scheduler.advanceTimeBy(500L, MILLISECONDS)

				subscriber.assertValues(
						listOf(
								INIT,
								Fire1
						),
						listOf(
								INIT,
								Fire1,
								Fire2
						),
						listOf(
								INIT,
								Fire1,
								Fire2,
								Action1
						),
						listOf(
								INIT,
								Fire1,
								Fire2,
								Action1,
								Action2
						)
				)
			}

		}

	}

	afterEach { RxPluginsPackage.reset() }

})
