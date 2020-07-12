/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.qos.server.handler;

import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.qos.command.CommandContext;
import com.alibaba.dubbo.qos.command.CommandExecutor;
import com.alibaba.dubbo.qos.command.DefaultCommandExecutor;
import com.alibaba.dubbo.qos.command.NoSuchCommandException;
import com.alibaba.dubbo.qos.command.decoder.HttpCommandDecoder;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;

/**
 * Parse HttpRequest for uri and parameters
 * <p>
 * <ul>
 * <li>if command not found, return 404</li>
 * <li>if execution fails, return 500</li>
 * <li>if succeed, return 200</li>
 * </ul>
 * <p>
 * will disconnect after execution finishes
 */
public class HttpProcessHandler extends SimpleChannelInboundHandler<HttpRequest> {

    private static final Logger log = LoggerFactory.getLogger(HttpProcessHandler.class);
    private static CommandExecutor commandExecutor = new DefaultCommandExecutor();

    private static final FullHttpResponse http_200(String result) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK,
                Unpooled.wrappedBuffer(result.getBytes()));
        HttpHeaders httpHeaders = response.headers();
        httpHeaders.set(HttpHeaders.Names.CONTENT_TYPE, "text/plain");
        httpHeaders.set(HttpHeaders.Names.CONTENT_LENGTH, response.content().readableBytes());
        return response;
    }

    private static final FullHttpResponse http_404() {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND);
        HttpHeaders httpHeaders = response.headers();
        httpHeaders.set(HttpHeaders.Names.CONTENT_TYPE, "text/plain");
        httpHeaders.set(HttpHeaders.Names.CONTENT_LENGTH, response.content().readableBytes());
        return response;
    }

    private static final FullHttpResponse http_500(String errorMessage) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.INTERNAL_SERVER_ERROR
                , Unpooled.wrappedBuffer(errorMessage.getBytes()));
        HttpHeaders httpHeaders = response.headers();
        httpHeaders.set(HttpHeaders.Names.CONTENT_TYPE, "text/plain");
        httpHeaders.set(HttpHeaders.Names.CONTENT_LENGTH, response.content().readableBytes());
        return response;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpRequest msg) throws Exception {
        CommandContext commandContext = HttpCommandDecoder.decode(msg);
        // return 404 when fail to construct command context
        if (commandContext == null) {
            log.warn("can not found commandContext url: " + msg.getUri());
            FullHttpResponse response = http_404();
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        } else {
            commandContext.setRemote(ctx.channel());
            try {
                String result = commandExecutor.execute(commandContext);
                FullHttpResponse response = http_200(result);
                ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
            } catch (NoSuchCommandException ex) {
                log.error("can not find commandContext: " + commandContext, ex);
                FullHttpResponse response = http_404();
                ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
            } catch (Exception qosEx) {
                log.error("execute commandContext: " + commandContext + " got exception", qosEx);
                FullHttpResponse response = http_500(qosEx.getMessage());
                ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
            }
        }
    }

}
