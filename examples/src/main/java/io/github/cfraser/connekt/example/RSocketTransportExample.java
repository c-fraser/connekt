/*
Copyright 2021 c-fraser

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package io.github.cfraser.connekt.example;

import io.github.cfraser.connekt.rsocket.AsyncQueueDestinationResolver;
import io.github.cfraser.connekt.rsocket.RSocketTransport;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class RSocketTransportExample {

  public static void main(String[] args)
      throws IOException, ExecutionException, InterruptedException {
    try (var transport =
        new RSocketTransport.Builder()
            .queueDestinationResolver(
                (AsyncQueueDestinationResolver)
                    queue ->
                        CompletableFuture.completedFuture(
                            Collections.singleton(new InetSocketAddress(8787))))
            .build()) {
      var receiveChannel = transport.receiveFrom("print-message");
      var sendChannel = transport.sendTo("print-message");
      var message = receiveChannel.receiveAsync().thenApply(String::new);
      sendChannel.sendAsync("Hello, world!".getBytes(StandardCharsets.UTF_8));
      System.out.println(message.get());
    }
  }
}
