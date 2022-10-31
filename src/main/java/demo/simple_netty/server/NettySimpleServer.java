package demo.simple_netty.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.*;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.ipfilter.*;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * @author yxl
 * @date 2022/11/1 上午3:18
 */
public class NettySimpleServer {
    public static void main(String[] args) throws InterruptedException {
        //1.创建BossGroup 和 WorkerGroup
        //1.1 创建2个线程组
        //bossGroup只处理连接请求
        //workerGroup 处理客户端的业务逻辑
        //2个都是无限循环
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        //2.创建服务端的启动对象,可以为服务端启动配置一些服务参数
        ServerBootstrap bootStrap = new ServerBootstrap();

        //2.1使用链式编程来配置服务参数
        bootStrap.group(bossGroup,workerGroup)                          //设置2个线程组
                .channel(NioServerSocketChannel.class)                 //使用NioServerSocketChannel作为服务器的通道
                .option(ChannelOption.SO_BACKLOG,128)            //设置线程等待的连接个数
                .childOption(ChannelOption.SO_KEEPALIVE,Boolean.TRUE) //设置保持活动连接状态
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    //给PipeLine设置处理器
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        //通过socketChannel得到pipeLine，然后向pipeLine中添加处理的handle

                        //编解码器 抽象的 待实现的
                        //TODO ByteToMessageDecoder
                        //用于将字节转为消息，需要检查缓冲区是否有足够的字节
                        //TODO ReplayingDecoder
                        //继承ByteToMessageDecoder，不需要检查缓冲区是否有足够的字节,
                        //但是 ReplayingDecoder速度略慢于ByteToMessageDecoder,同时不是所有的ByteBuf都支持。
                        //项目复杂性高则使用ReplayingDecoder，否则使用ByteToMessageDecoder
                        //TODO MessageToMessageDecoder
                        //用于从一种消息解码为另外一种消息（例如POJO到POJO）
                        socketChannel.pipeline().addLast(new StringDecoder());//字符串解码器 基于MessageToMessageDecoder
                        socketChannel.pipeline().addLast(new StringEncoder());//字符串编码器 基于MessageToMessageDecoder


                        //IP过滤器
                        //自定义拦截器规则
                        socketChannel.pipeline().addLast(new RuleBasedIpFilter(new IpFilterRule() {
                            @Override
                            public boolean matches(InetSocketAddress inetSocketAddress) {
                                //bala bala bala
                                return false;
                            }

                            @Override
                            public IpFilterRuleType ruleType() {
                                return null;
                            }
                        }));

                        //netty提供的针对拆包和粘包的四个解决方案

                        //固定长度的拆包器 按照固定长度进行拆分
                        socketChannel.pipeline().addLast(new FixedLengthFrameDecoder(100));
                        //行拆包器 按照换行符进行拆分  输入一个包的最大长度
                        socketChannel.pipeline().addLast(new LineBasedFrameDecoder(100));
                        //分隔符拆包器 通过自定义分隔符进行拆包 输入一个包的最大长度 还有自定义分隔符
                        socketChannel.pipeline().addLast(new DelimiterBasedFrameDecoder(100,
                                Unpooled.copiedBuffer("!=!".getBytes(StandardCharsets.UTF_8))));
                        //基于数据包长度的拆分器
                        //第一个参数是一个数据包最大长度
                        //第二个参数是获取数据长度的起始位置
                        //第三个参数是这个数据多长
                        socketChannel.pipeline().addLast(new LengthFieldBasedFrameDecoder(1000,0,4));



                        socketChannel.pipeline().addLast(new NettyFirstHandle());
                    }
                }); //给workerGroup 的EventLoop对应的管道设置处理器(可以自定义/也可使用netty的)
        System.err.println("server is ready......");

        //启动服务器，并绑定1个端口且同步生成一个ChannelFuture 对象
        ChannelFuture channelFuture = bootStrap.bind(8888).sync();

        //对关闭通道进行监听(netty异步模型)
        //当通道进行关闭时，才会触发这个关闭动作
        channelFuture.channel().closeFuture().sync();
    }
}
