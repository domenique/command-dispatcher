/*
 * Copyright 2022 TripleD framework.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.tripledframework.eventbus.internal.infrastructure.outboxinvoker;

import eu.tripledframework.eventbus.internal.infrastructure.invoker.TestInvokerWithoutResponse;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class OutboxInvokerDecoratorTest {

  @Test
  void notCallRealInvoker() {
    var invoker = new TestInvokerWithoutResponse();
    var decorated = OutboxInvokerDecorator.decorate(invoker, new TestOutbox());

    decorated.invoke("theCommand");

    assertThat(invoker.isInvokeCalled, is(false));
  }
}
