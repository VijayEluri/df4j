/*
 * Copyright 2011 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.github.rfqu.df4j.util;

import com.github.rfqu.df4j.core.OutPort;

/**
 * Unary operation
 *
 * @param <T> type of the operand
 * @param <R> type of the result
 */
public abstract class UnaryOp<T> extends Operation<T> implements OutPort<T> {
    protected Input<T> input=new Input<T>();

    @Override
    public void send(T value) {
        input.send(value);
    }

    @Override
    public void run() {
        send(operation(input.operand));
    }

    abstract protected T operation(T opnd);

}