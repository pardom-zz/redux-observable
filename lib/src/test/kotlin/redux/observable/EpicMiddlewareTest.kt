package redux.observable

import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith
import redux.INIT
import redux.api.Reducer
import redux.api.Store
import redux.applyMiddleware
import redux.asObservable
import redux.createStore
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

@RunWith(JUnitPlatform::class)
class EpicMiddlewareTest : Spek({

    beforeEachTest { RxPluginsPackage.setUp() }

    describe("EpicMiddleware") {

        describe("create") {

            it("should accept a epic argument that wires up a stream of actions to a stream of actions") {
                val reducer = Reducer { state: List<Any>, action: Any -> state + action }

                val epic = Epic { actions: Observable<out Any>, store: Store<List<Any>> ->
                    Observable.merge(
                        actions.ofType(Fire1::class.java).map { Action1 },
                        actions.ofType(Fire2::class.java).map { Action2 }
                    )
                }

                val store = createStore(reducer, emptyList(), applyMiddleware(createEpicMiddleware(epic)))

                store.dispatch(Fire1)
                store.dispatch(Fire2)

                expect(listOf(
                    INIT,
                    Fire1,
                    Action1,
                    Fire2,
                    Action2

                )) { store.state }
            }

            it("should allow you to replace the root epic with replaceEpic") {
                val reducer = Reducer { state: List<Any>, action: Any -> state + action }

                val epic1 = Epic { actions: Observable<out Any>, store: Store<List<Any>> ->
                    Observable.merge(
                        actions.ofType(Fire1::class.java).map { Action1 },
                        actions.ofType(Fire2::class.java).map { Action2 },
                        actions.ofType(FireGeneric::class.java).map { Epic1Generic }
                    )
                }

                val epic2 = Epic { actions: Observable<out Any>, store: Store<List<Any>> ->
                    Observable.merge(
                        actions.ofType(Fire3::class.java).map { Action3 },
                        actions.ofType(Fire4::class.java).map { Action4 },
                        actions.ofType(FireGeneric::class.java).map { Epic2Generic }
                    )
                }

                val middleware = createEpicMiddleware(epic1)
                val store = createStore(reducer, emptyList(), applyMiddleware(middleware))

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

                )) { store.state }
            }

            it("should dispatch actions mapped to other threads") {
                val scheduler = TestScheduler()
                val subscriber = TestSubscriber<List<Any>>()

                val reducer = Reducer { state: List<Any>, action: Any -> state + action }

                val epic = Epic { actions: Observable<out Any>, store: Store<List<Any>> ->
                    // Simulate network requests
                    val networkRequest1 = Observable.just(Action1)
                        .subscribeOn(scheduler)
                        .delay(100L, MILLISECONDS, scheduler)
                    val networkRequest2 = Observable.just(Action2)
                        .subscribeOn(scheduler)
                        .delay(200L, MILLISECONDS, scheduler)

                    Observable.merge(
                        actions.ofType(Fire1::class.java).flatMap { networkRequest1 },
                        actions.ofType(Fire2::class.java).flatMap { networkRequest2 }
                    )
                }

                val store = createStore(reducer, emptyList(), applyMiddleware(createEpicMiddleware(epic)))
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

    afterEachTest { RxPluginsPackage.reset() }

})
