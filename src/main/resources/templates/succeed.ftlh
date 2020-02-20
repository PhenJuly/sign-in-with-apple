<#import "/spring.ftl" as spring>
<html>
<h1>sign in with apple callback info</h1>
<ul>
    授权回调返回：
    <li>code:${code}</li>
    <li>id_token:${id_token}</li>
    <li>state:${state}</li>
</ul>

<ul>
    decode回调id_token:
    <li>header:${decode_id_token_header}</li>
    <li>body:${decode_id_token}</li>
</ul>

<ul>
    后台验证结果:
    <li>JWT-VERIFY:     ${verificationForJwt}</li>
    <li>CODE-VERIFY:    ${verificationForCode}</li>
</ul>

</html>