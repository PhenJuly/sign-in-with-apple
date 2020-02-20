# Sign in with Apple

Sign In With Apple 对苹果系统来说便是使用苹果系统自带的AppleID账户授权应用程序，使其无需填写电话，个人信息等进行注册提交即可访问应用程序，可以看成是和微信类似的第三方账号。

Sign in with Apple 支持跨平台：

- Native SDK 支持 iOS/MacOS/watchOS/tvOS
- Javascript SDK 支持  Android, Windows, Web

注意：此项目不包含app端实现。

## web 授权

### 开发者账号申请

到苹果开发者网站注册开发者账号，地址：https://developer.apple.com/account/

我申请到一半发现要钱，还不便宜，所以没走下去，按着上面流程说明付完钱应该就可以了。

### 后端服务配置

前提：已经持有苹果开发者账号

也可以直接看官网：https://developer.okta.com/blog/2019/06/04/what-the-heck-is-sign-in-with-apple

首先，进入开发者账号页面，点击： **Certificates, Identifiers and Profiles**,进入配置页面

![1582184162963](img/开发者账号页面.png)

选择：Identifiers ，点击Identifiers 后面+号，创建 应用或者服务

![1582184333192](img/Identifiers.png)

这里选择Service IDS:

如果要是IOS app开发，则选择APP IDS,因为本文主要讲解web授权，因此是Services ID )

![1582184426394](img/RegisterNewIdentifier.png)

添加注册服务信息：

1. 添加注册服务的描述，唯一标识

2. 选择 sign in with Apple 

3. 点击 configurate按钮就配置 授权信息

   1. 选择APPID ,这个为下拉选择，主要您在APPID 中注册了 应用，且选择了sign in with Apple 这里就会出现

   2. domains 下面填写一个您有权限的域（起始就是回调地址中的域名）和回调地址

      domains说明：

      这里保存 创建后，再次进来，会出现下载download和校验verify 按钮，按照要求下载文件，上传到上面指定的地址后点击verify 校验 ，成功即可。如https://你的服务域名/.well-known/apple-developer-domain-association.txt 

      这个verify 可能对生产环境有影响，本人测试黄金验证从来没成功过，但是不影响后面的授权，所以如果没有https域名服务，自己玩可以先不管这一步。

   3. 都配置好 点击 保存save ,继续  continue ，注册 Register即可

![1582184563458](img/RegisterServicesID.png)

![1582184663341](img/WebAuthenticationConfiguration.png)

![1582185166632](img/updateWebAuthenticationConfiguration.png)

创建服务后要创建服务的私钥（后续调用apple服务验证授权码的时候需要用，如果您不验证token有效性等应该不需要配置）

选择左边Keys 廊，点击+按钮，添加keys

![1582185481797](img/keys.png)

配置key信息：

1. 设置key名称

2. 勾选 sign in with apple 

3. 点击configure ，选择key的appID（同service创建时一样）

4. 点击Register创建成功后会出现一个Download Your Key页面，点击下载保存（仅可以下载一次，一定要妥善保管，里面是一串编码过后的字符，可以修改文件后缀为txt查看）：

   ```
   -----BEGIN PRIVATE KEY-----
   xxxx 中间为实际的密钥
   -----END PRIVATE KEY-----
   ```

![1582185598702](img/RegisterNewKey.png)

![1582185674541](img/ConfigureKey.png)

![1582185782129](img/DownloadYourKey.png)



配置完以上后台配置即可进行web 授权项目开发

### 代码实现

1. 在您的html页面中 引入js文件：https://appleid.cdn-apple.com/appleauth/static/jsapi/appleid/1/en_US/appleid.auth.js

2. 在head中指定授权信息 

   1. appleid-signin-client-id，为创建service服务的时候输入的 Identifier

   2. appleid-signin-scope 可以填多个，例如name email，其他的没看到官网有写

   3. appleid-signin-redirect-uri 授权回调地址，前面注册service配置的地址

      以上信息也可以不写死在页面，通过AppleID.auth.init 配置获取接口返回自动刷新（仔细看后面示例）

配置完以上地址，访问网页即可看到页面中出现 sign in with apple 登录按钮

后续开发回调接口接口回调结果即可：

POST 请求，application/x-www-form-urlencoded格式，主要返回数据有：

>code：授权码
>
>id_token：一个JWT格式的字符，里面包含了用户的唯一标识sub,有效时间等其他信息，可以直接用.号分割，再Base64解析即可看到header，body中的详细信息,解析后大概
>
>```
>{"iss":"https://appleid.apple.com",
>"aud":"xxx 服务标识，appId 或者client_id",
>"exp":1582183330,"iat":1582182730,
>"sub":"xx.xxx.xx 用户唯一标识，同客户端返回的userId是一样",
>"c_hash":"1uN9Y8T2MBG9AF2RkqtaWw",
>"email":"nx8usdwihy@privaterelay.appleid.com","email_verified":"true","is_private_email":"true",
>"auth_time":1582182730,"nonce_supported":true}
>```
>
>state：请求时配置的appleid-signin-state
>
>user：用户信息，登录流程中输入用户和邮件的信息：
>
>​	{ "name": { "firstName": string, "lastName": string }, "email": string }

```html
<html>
<head>
    <meta charset="UTF-8"> <!-- for HTML5 -->
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
    <meta name="appleid-signin-client-id" content="xxxxxx">
    <meta name="appleid-signin-scope" content="name email">
    <meta name="appleid-signin-redirect-uri" content="http://xxx/callback">
    <meta name="appleid-signin-state" content="1234567">
    <!--  <meta name="appleid-signin-use-popup" content="true"> 为true则会新起一个页面 并且授权成功后没有回调，页面动也不懂，不知道是不是新的模式，没搞清楚 所以直接注释掉 -->
    <style type="text/css">
        #apple-div {
            padding-left: 30%;
            margin: 0 auto;
        }
    </style>
</head>
<body>
<div id="apple-div">
    <div id="appleid-signin" data-color="black" data-border="true" data-type="sign in"></div>
</div>
<script type="text/javascript"
        src="https://appleid.cdn-apple.com/appleauth/static/jsapi/appleid/1/en_US/appleid.auth.js"></script>
</body>
</html>
```

```html
<html>
    <head>
    </head>
    <body>
        <script type="text/javascript" src="https://appleid.cdn-apple.com/appleauth/static/jsapi/appleid/1/en_US/appleid.auth.js"></script>
        <div id="appleid-signin" data-color="black" data-border="true" data-type="sign in"></div>
        <script type="text/javascript">
            AppleID.auth.init({
                clientId : '[CLIENT_ID]',
                scope : '[SCOPES]',
                redirectURI : '[REDIRECT_URI]',
                state : '[STATE]',
                usePopup : true //or false defaults to false
            });
        </script>
    </body>
</html>
```



## 后台验证

后台验证主要有两种验证方式：

1. 验证授权返回的JWT格式的id_token
   1. 调用https://appleid.apple.com/auth/keys获取公钥
   2. 根据公钥验证id_token
2. 授权码验证
   1. 调用https://appleid.apple.com/auth/token验证授权码

### JWT验证

​	哎，算了，不写了，大家看代码吧。

### 授权码验证

## 温馨提醒

1. web 授权的时候html head配置的appleid-signin-use-popup属性最好为false或者不配置，为true时，点击登录会新出现一个页面，授权成功后关闭页面，但是没有调用回调接口，导致我也很懵逼，不知道是啥模式
2. 创建service是配置域名，domain的校验不成功的话不要太过纠结，不影响授权流程，生产就不知道了
3. JWT验证id_token时,调用apple 分获取公钥接口会返回一个key列表（接口没参数），需要根据需要取其中一个
   1. 试了下，需要先对id_token header部分解码，获取kid，再根据kid获取公钥列表中对应的公钥信息，再进行解码（这流程不太科学，但是验证了只有用列表中kid和id_token header部分中kid一样的公钥信息才能验证成功，读者要是知道咋回事，望告知一声，谢谢）
   2. 

