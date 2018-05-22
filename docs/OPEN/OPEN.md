[TOC]

## 版本

  `1.2`

## 修改日志

* 2016-03-21

  - 修改**用户设备号绑定**与**用户设备号解除绑定** 的接口返回。
  
* 2016-03-23
  - 重命名参数 `sessionId` 为 `sid`
  - 增加获取sessionid的接口

* 2016-03-24
   - 修改 `server.time`, `get.user.captcher`, `get.session.id`等接口的返回值
   - 增加用户登录调用备注事项
   - 取消设备号绑定接口（解绑会在用户登出时自动执行）
   - 增加用户登出操作

## 开发约定

### 编码

若无特殊说明，编码请统一使用 `UTF-8`

### 授权
在使用开放接口前请先行联系管理员索取调用时所需的 `appKey` 和 `appSecret`


### 接口通用参数

接口通用参数是指在调用开放平台时必须传入的参数， 如 `pamapsCall` 、`sign` 等， 除特殊指定接口外任何都会获得形如 `xxxx.miss` 的异常响应

+ pampasCall
   调用公共的方法名
+ appKey
   用户调用开放平台接口时所需的身份验证信息
+ appSecret
    用户调用平台开放接口时需要参与签名运算的密钥
+ timestamp
    (格式为: yyyyMMddHHmmss) 在调用电商平台接口时会额外需要传入App的时间， 当超出调用的时间窗口时候会获得 `invoke.expired` 的错误响应。 除获取用户时间接口

### 签名方式

所有调用参数(包括pampasCall, 但不包括sign本身), 按照字母序升序排列, 然后调用者再附加上给分配给自己的appSecret, 再做md5签名

示例：

> /api/gateway?appKey=foobar&pampasCall=say.hi&name=dadu&timestamp=20160320133000

1. 则首先参数名按照字母序升序排列, 得到

  > appKey=foobar&name=dadu&pampasCall=say.hi&timestamp=20160320133000

2. 再假设分配给客户的appSecret为my.secret, 则将其附加到参数末尾, 得到

  > appKey=foobar&name=dadu&pampasCall=say.hi&timestamp=20160320133000my.secret

3. 再计算这段字符串的md5,得到校验码为```6c63513c10ab22f9fd9448719a989ef5```, 所以最后的请求为
  
  > api/gateway?appKey=foobar&name=dadu&pampasCall=say.hi&timestamp=20160320133000&sign=6c63513c10ab22f9fd9448719a989ef5



### 通用错误清单

+ appKey.miss(400): AppKey不能为空
+ appKey.incorrect(400): 非法的AppKey
+ sign.mismatch:(400): 签名未通过
+ timestamp.miss(400): 时间戳不能为空
+ timestamp.incorrect(400): 错误的时间戳格式
+ clientInfo.miss(400): ClientId信息不能为空
+ permission.deny(401): 未获取授权
+ invoke.expired(403): 调用已逾期
+ method.target.not.found (404):  未找到指定方法
+ method.not.allowed (405)：错误的Http请求方法
+ server.internal.error(500):  服务异常

## 公共模块

### 获取当前服务器时间(server.time)

----

  获取当前服务器时间



* **URL**

> - 为方便阅读参数进行换行，在实际调用中请勿加入换行符
> - 此接口无需传入timestamp参数

    /api/gateway?
         appKey=[MY_KEY]
         &pampasCall=server.time
         &sign=[SIGN]

* **HTTP 方法**

  `GET` 
  
* **接口方法**
   
   `server.time`

*  **URL 参数**

 无

* **成功响应 **
             
 * **Code: **  200 <br />
    **Content: **
   
            {
			  "success": true,
			  "result": {
			    "time": "20160324091804"
			  }
			}

* **错误清单**

    无




## 用户模块

### 获取用户会话id(get.session.id)

---

* **URL** 

> 为方便阅读参数进行换行，在实际调用中请勿加入换行符

    api/gateway?
        appKey=[MY_KEY]
        &pampasCall=get.session.id
	    &key=[UNIQUE_KEY]
        &timestamp=20160301123015
        &sign=[SIGN]

* **HTTP 方法**

  `GET` 


* **接口方法**
   
   `get.session.id`

*  **URL 参数**

 + 必填项[REQUIRED]
 
         - **key=[string]**  
         能确保唯一的key，可以是APP的设备号
        
         
* **成功响应 **
             
 * **Code: **  200 <br />
    **Content: **
   
	        {
			  "success": true,
			  "result": {
			    "sessionId": "9c39acfcZ178122f2Z153a633c77aZ80fc"
			  }
			}
 
* **响应参数说明**

 - **result=[string]**  
    系统随机生成的sessionid



* **失败响应**

  * **Code: ** 400  <br />
    **Content: ** 
                
            {
              "success": false,
              "error": "key.miss",
              "errorMessage": "Key不能为空"
            }

* **错误清单**

    + key.miss: Key不能为空
   



### 用户名密码登录(user.login)

----

* **URL** 

> 为方便阅读参数进行换行，在实际调用中请勿加入换行符

    api/gateway?
        appKey=[MY_KEY]
        &pampasCall=user.login
        &name=LEE
        &password=PASSWORD
        &type=1
        &sid=1
        &code=VERIFY_CODE
        &timestamp=20160301123015
        &sign=[SIGN]



* **HTTP 方法**

  `POST` 
  
* **接口方法**
   
   `user.login`

*  **URL 参数**

 + 必填项[REQUIRED]
 
         - **name=[string]**  
         登录用户名（登录名/手机号/邮箱)
         - **password=[string] **
         登录密码明文， **如果不使用https进行传输，建议对接双方约定一种可逆算法进行加解密**
         - **type=[int]**
             1. 登录名
             2. 邮箱
             3. 手机
             4. 其他
         - **code=[string]**
         图片验证码(当登录失败3次以上时要求必传）
         
         

* **成功响应 **
             
 * **Code: **  200 <br />
    **Content: **
   
            {
                "success": true,
                "result": 
                {
                    "name": "lee",
                    "expiredAt": "20160324203320",
                    "sid": "cbd8449cZ15fe2c9cZ153823ac0cbZ9a65",
                    "domain": "xxxx.com"
                }
            }

 
* **响应参数说明**

 - **name=[string]**  
    登录时的用户名
 - **expiredAt**
    凭证失效日期 (格式: yyyyMMddHHmmss)
 - **sid**
    登录凭证
 - **domain**
     凭证有效的域名



* **失败响应**

  * **Code: ** 400  <br />
    **Content: ** 
                
            {
              "success": false,
              "error": "user.name.miss",
              "errorMessage": "用户名不能为空"
            }

* **错误清单**

    + user.name.miss: 用户名不能为空
    + user.password.miss: 密码不能为空
    + user.code.miss: 验证码不能为空
    + login.fail: 用户名、密码或验证码错误
    + user.status.locked(400)=用户被锁定
	+ user.status.frozen(400)=用户被冻结
	+ user.status.deleted(400)=用户被删除
	+ user.status.abnormal(400)=用户状态非法
	+ user.password.mismatch(400)=用户密码错误


### 获取用户验证码(get.user.captcher)

根据sid获取用户验证吗，响应

----

* **URL** 

> 为方便阅读参数进行换行，在实际调用中请勿加入换行符

    api/gateway?
        appKey=[MY_KEY]
        &pampasCall=get.user.captcher
        &sid=cbd8449cZ15fe2c9cZ153823ac0cbZ9a65
        &timestamp=20160301123015
        &sign=[SIGN]


* **HTTP 方法**

  `GET` 
  
* **接口方法**
   
   `get.user.captcher`

*  **URL 参数**

 + 必填项[REQUIRED]
 
         - **sid=[string]**  
         登录所获取的session凭证
         
         

* **成功响应 **
             
 * **Code: **  200 <br />
    **Content: **
   
	        {
		      "success": true,
              "result": {
		         "captcher": "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"
		      }
		    } 

* **失败响应**

  * **Code: ** 400  <br />
    **Content: ** 
                
            {
              "success": false,
              "error": "session.id.miss",
              "errorMessage": "session id 不能为空"
            }

* **错误清单**

    + session.id.miss: session id 不能为空



### 用户设备号绑定(user.device.bind)

----

* **URL** 

> 为方便阅读参数进行换行，在实际调用中请勿加入换行符

    api/gateway?
        appKey=[MY_KEY]
        &pampasCall=user.device.bind
        &sid=cbd8449cZ15fe2c9cZ153823ac0cbZ9a65
        &deviceToken=[APP_DEVICE_TOKEN]
        &timestamp=20160301123015
        &sign=[SIGN]



* **HTTP 方法**

  `POST` 
  
* **接口方法**
   
   `user.device.bind`

*  **URL 参数**

 + 必填项[REQUIRED]
 
         - **sid=[string]**  
         登录所获取的session凭证
         - **deviceToken=[string]**  
         移动端设备号
        
         

* **成功响应 **
             
 * **Code: **  200 <br />
    **Content: **
   
            {
                "success": true
            }

 

* **失败响应**

  * **Code: ** 400  <br />
    **Content: ** 
                
            {
              "success": false,
              "error": "user.not.login",
              "errorMessage": "用户名不能为空"
            }

* **错误清单**

    + user.not.login: 用户未登录


### 用户登出(user.logout)

----

* **URL** 

> 为方便阅读参数进行换行，在实际调用中请勿加入换行符

    api/gateway?appKey=[MY_KEY]
			    &pampasCall=user.logout
			    &sid=68dc3619be4a636d59b4cc4cc5fcdb29
				&deviceToken=[APP_DEVICE_TOKEN]
				&sign=[SIGN]
				&timestamp=20160322194300



* **HTTP 方法**

  `POST` 
  
* **接口方法**
   
   `user.logout`

*  **URL 参数**

 + 必填项[REQUIRED]
 
         - **sid=[string]**  
         登录所获取的session凭证
       

* **成功响应 **
             
 * **Code: **  200 <br />
    **Content: **
   
            {
                "success": true
            }

 

* **失败响应**

  * **Code: ** 400  <br />
    **Content: ** 
                
            {
              "success": false,
              "error": "user.device.unbind.fail,
              "errorMessage": "用户名不能为空"
            }

* **错误清单**

    + user.device.unbind.fail 设备号解绑失败
