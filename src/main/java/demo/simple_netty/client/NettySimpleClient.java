package demo.simple_netty.client;

import demo.simple_netty.server.NettyFirstHandle;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

/**
 * @author yxl
 * @date 2022/11/1 上午3:19
 */
public class NettySimpleClient {
    public static void main(String[] args) {
        //1.客户端定义一个循环事件组
        EventLoopGroup group = new NioEventLoopGroup();

        try {

            //2.创建客户端启动对象
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)                      //设置线程组
                    .channel(NioSocketChannel.class)   //设置客户端通道实现类
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            socketChannel.pipeline().addLast(new NettyFirstClientHandle());
                        }
                    });
            System.err.println("client is ready......");

            //3.启动客户端去连接服务端
            ChannelFuture channelFuture = bootstrap.connect("127.0.0.1", 8888).sync();

            //4.设置通道关闭监听(当监听到通道关闭时，关闭client)
            channelFuture.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            group.shutdownGracefully();
        }
    }
}
