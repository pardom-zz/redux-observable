package redux.observable.helpers

import redux.observable.helpers.ActionCreators.Action.Action1
import redux.observable.helpers.ActionCreators.Action.Action2
import redux.observable.helpers.ActionCreators.Action.AsyncAction1
import redux.observable.helpers.ActionCreators.Action.AsyncAction2
import redux.observable.helpers.ActionCreators.Action.Fire1
import redux.observable.helpers.ActionCreators.Action.Fire2

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

object ActionCreators {

	sealed class Action {
		object Fire1 : Action()
		object Fire2 : Action()
		object Action1 : Action()
		object Action2 : Action()
		object AsyncAction1 : Action()
		object AsyncAction2 : Action()
	}

	fun fire1() = Fire1

	fun fire2() = Fire2

	fun action1() = Action1

	fun action2() = Action2

	fun asyncAction1() = AsyncAction1

	fun asyncAction2() = AsyncAction2

}
