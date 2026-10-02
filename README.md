# 🪐 口木科技-plugin

**如果你在服务器中修改了口木科技并出现了问题，请不要找我——修改后的版本作者概不负责。**

口木科技是基于 [RykenSlimefunCustomizer](https://builds.guizhanss.com/SlimefunReloadingProject/RykenSlimeCustomizer/main)（简称 RSC）开发的粘液科技附属，为原版玩法注入了修仙元素，包含灵石体系、法则悟道、身外身、灵杖、卷轴等内容。
现在已经被我**使用ai**转成了jar😋
提供了更好的性能

---

## 📦 安装方法

1. 在release或者资源站（暂时还没上传）下载最新版口木科技放入 `plugins` 文件夹。
2. 确保前置插件齐全（见下文）。
3. 启动服务器，安装完成。

---

## 🔧 前置插件

| 类型 | 插件 |
|------|------|
| **必需** | [Slimefun](https://github.com/Slimefun/Slimefun4) |
| **必需** | [GuizhanLibPlugin](https://github.com/ybw0014/GuizhanLibPlugin) |
| **必需** | [RykenSlimefunCustomizer](https://builds.guizhanss.com/SlimefunReloadingProject/RykenSlimeCustomizer/main) |
| **可选** | [NetworkTechnology](https://github.com/balugaq/NetworkTechnology)（用于材质网桥相关物品） |

---

## 🧪 使用提示

- **数值适配**：口木科技包含灵杖、卷轴、符箓等道具，建议服主根据服务器实际情况调整数值。
- **非公开脚本**：部分功能（如堆叠光暗）为非公开脚本，如有需要请联系作者获取授权。
- **交易限制**：口木科技内的物品**不建议**用于服务器内的大米交易（非金币），请尊重作者的创作意图。

---

## 👤 关于作者

口木科技由 [**口木A（Komu_A）**](https://github.com/KomuAaA) 独立开发，致力于为粘液科技玩家提供全新的修仙体验。  
当前支持 Minecraft **1.21+** 版本，后续将持续更新。

> 🔒 口木科技包含非开源内容，任何未经授权的传播、倒卖均不被允许。  
> 如果你通过非官方渠道获取了本附属，请确保已获得作者授权。

---

## 📬 反馈与联系

- 提交 Issue：[GitHub Issues](https://github.com/ykcgly/Komutech-Plugin/issues)  
- 口木的 QQ：`2819696145`（添加时请注明来意）
- 养坤场管理员的QQ：`1424136122`
  
感谢你的使用与支持！✨

## 一些碎碎念

当初最早是有一个人给口木转成了jar版本，但是在我随后的测试中发现，他其实只是个js加载器（钢管落地）
单独安装这个版本就相当于一个只加载口木科技的rsc...
而且搞笑的是，当检测到服务器有安装rsc的时候，他会解压自带的口木科技，然后扔进rsc的addons文件夹
请输入文本
这个jar还没有开源，源代码是我反编译出来的
搞半天是个rsc伪装的，气死我了
之前口木科技作者也自己承认了附属存在后门....
对此我只能说跟那个冷殇一桌🤣
但是，后面承认的态度比较好，那次事件过后也就没有继续追究
当然，本jar附属已经完整去除了所有后门，大可放心使用