(ns share.dicts.zh-cn)

(def dicts
  {:simplified_chinese "简"
   :traditional_chinese "繁"
   :japanese "日"
   :english "英"
   :german "德"
   :french "法"
   :spanish "西班牙"
   :turkish "土耳其"
   :italian "意大利"
   :portuguese "葡萄牙"
   :swedish "瑞典"
   :danish "丹麦"
   :norwegian "挪威"
   :dutch "荷兰"
   :polish "波兰"
   :indonesian "印尼"
   :hindi "印地"
   :russian "俄"
   :korean "韩"
   :thai "泰"
   :arabic "阿拉伯"
   :bengali "孟加拉"
   :punjabi "旁遮普"
   :switch-to "切换为"
   :hot "热门"
   :latest "最新"
   :latest-posts "文章"
   :latest-comments "评论"
   :expand "展开"
   :collapse "收回"
   :submit "提交"
   :cancel "取消"
   :report "举报"
   :delete "删除"
   :reply "回复"
   :comment "评论"
   :post "文章"
   :title "文章标题"
   :reason "原因"
   :user "用户"
   :ignore-it "不管"
   :disable-user "禁言三天"
   :block-user "加入黑名单"
   :no-more-reports "暂时没有更多举报了"
   :no-more-notifications "没有未读通知"
   :general "默认"
   :purpose "描述"
   :rules "规则"
   :optional "可不填"
   :new-message "新的消息"
   :create "创建"
   :your-thoughts-here "你的想法是什么?"
   :post-comment "发布评论"
   :back "返回"
   :preview "预览"
   :like-it "喜欢"
   :unlike-it "取消喜欢"
   :like "喜欢"
   :unlike "取消喜欢"
   :edit "编辑"
   :update "修改"
   :replies "条评论"
   :invite "邀请朋友"
   :signin "登录"
   :slogan "Where programmers discuss articles, books and papers."
   :members "成员"
   :admins "管理员"
   :support "支持"
   :contact "联系我们"
   :privacy "隐私"
   :code-of-conduct "社区规则"
   :email "邮箱"
   :invalid-email "无效邮箱。"
   :signin-github "Github 登录"
   :signup-email "邮箱注册"
   :invalid-code "无效的验证码"
   :please-input-verify-code "请输入验证码"

   ;; notifications
   :as-below-deleted " 被删除了。"
   :disable-account-notification "你的账号已被禁言三天， 小组: "
   :block-notification "你已被加入黑名单:  "
   :promote-notification "提升你为管理员: "
   :new-comment "新评论: "
   :post-new-comment " 有新的评论"
   :new-comment-to-my-comment "回复了你的评论"
   :dismiss-all "清空"

   ;; post
   :photo-upload "上传图片， 一次最多可以上传 9 张图片"
   :post-title-warning "标题长度大于 4 且不超过 64 个字符。"
   :post-tags-warning "Tags 不能为空。"
   :post-body-placeholder "内容..."
   :publish "发布"
   :settings "设置"
   :publish-to "发布文章"
   :vote "顶"
   :unvote "取消"
   :down "踩"
   :undown "取消"
   :no-more-posts "没有更多了。"
   :more "更多"
   :be-the-first "写我的感想"
   :no-posts-yet "暂时没有文章。"
   :no-comments-yet "暂时没有评论。"
   :no-drafts-yet "暂时没有草稿。"
   :link "链接"
   :close "关闭"
   :post-permalink-copied "文章链接已复制!"
   :comment-permalink-copied "评论链接已复制!"
   :tweet "发推"
   :share "分享"
   :add "添加"
   :post-not-available "当前文章不存在。"

   ;; root
   :search "搜索"
   :search-posts "搜索文章..."
   :reports "举报"
   :write-new-post "写文章"
   :new-post-description "写一篇新的文章。"
   :root-title "Where programmers discuss articles, books and papers. - Putchar"
   :root-description "Putchar.org 是一个纯粹的技术讨论和分享社区。大家可以在这里讨论和分享文章，书籍和论文。"
   :go-to-profile "文章"
   :profile-updated "已保存!"
   :sign-out "注销"

   ;; search
   :search-result "搜索结果: "
   :empty-search-result "没有匹配的文章。"

   ;; user
   :screen-name-taken "用户名已经被占用。"
   :username-length-warning "用户名长度不可以超过 15 个字符。"
   :email-exists "邮箱已存在。"
   :full-name "姓名"
   :full-name-placeholder "姓名, 可不填"
   :full-name-warning "姓名不可以为空。"
   :unique-username "用户名"
   :bio "简介"
   :bio-placeholder "介绍下自己吧， 可不填。"
   :click-circle-add-avatar "添加头像"
   :languages "语言"
   :welcome "欢迎"
   :signup "注册"
   :your-email "你的邮箱:"
   :cached-change-avatar "你的头像已经修改， 需要重新刷新页面。"
   :change-avatar "修改头像"
   :update-profile "修改个人设置"
   :votes "顶"
   :my-votes "顶过的文章"
   :my-drafts "草稿"
   :old "默认排序"
   :new "最新"
   :best "赞"

   :spam "这是一篇垃圾文章/评论"
   :abusive "谩骂， 侮辱 或者骚扰"
   :other-issues "其他问题"

   ;; handlers
   ;; comment
   :new-comment-received "有新的评论!"
   :bad-happened "操作失败， 请稍后重新尝试， 或者联系我们。"

   :post-published "文章已发布， :)"
   :post-updated "文章已更新!"
   :post-deleted "文章已删除。"
   :post-delete-confirm "你确定要删除这篇文章吗?"

   ;; report
   :report-sent "举报已发送， 谢谢!"

   ;; user
   :please-check-your-email "请查收你的邮箱!"

   :username-exists "用户名已经被占用。"
   :pick-a-username "用户名"
   :username "用户名"

   ;; time
   :d "天前"
   :h "小时前"
   :m "分钟前"
   :s "秒"
   :now "刚刚"

   :export-my-data "导出我的数据"
   :delete-this-account "永久删除账号"
   :invites-sent "邀请已发送!"
   :twitter-handle "Twitter 用户名"
   :github-handle "Github 用户名"
   :public "公开"
   :description "简介"
   :upload-a-cover "上传封面"
   :cover "封面"

   :bugs "Bug 反馈"
   :feature-requests "其他功能"
   :safety "隐私安全"
   :social "社交"
   :team-support "支持"
   :notifications "通知"
   :star "收藏"
   :unstar "取消收藏"
   :about "关于"
   :back-to-top "回到顶部"
   :set-as-cover "设为封面"
   :save "保存"
   :upload "上传"
   :no-replies-yet "暂时还没有评论。"
   :back-to "返回 "
   :not-found "你来到了未知地带。"
   :draft "草稿"
   :drafts "草稿"
   :saving "保存中..."
   :sort "排序"
   :latest-reply "最近回复"
   :created-at "创建于"
   :last-reply-at "最近回复"
   :updated-at "上次修改时间"
   :loading "加载中..."
   :contact-us "联系我们"
   :website-name "Putchar"
   :popular "热门话题"
   :new-created "最新"
   :welcome-to-putchar "欢迎来到 Putchar.org!"
   :activate-your-account "登录"
   :last-reply-by "最近回复: "
   :frequent-poster "最多讨论: "
   :posted-by "提交: "
   :email-login-placeholder "你的邮箱地址"
   :report-this-post "举报"
   :delete-this-post "删除"
   :report-this-comment "举报"
   :delete-this-comment "删除"

   ;; post tags
   :tags "Tags"
   :add-tags-label "添加标签(最少 1 个):"
   :add-tags "最多 3 个标签， 逗号分隔..."
   :email-notification-settings "邮件通知"
   :email-notification-settings-text "当别人回复我或者提及我的用户名邮件通知我"
   :my-data "我的数据"
   :about-text "

## 关于 Putchar.org
Putchar.org 是一个纯粹的技术社区, 大家可以在这里讨论和分享文章, [书](/books) 还有 [论文](/papers)。

### 功能

* 用户可以添加或收藏自己喜欢的书和论文。

  比如 [Purely functional data structures](/book/7)。

* 文章支持多种格式，包括 Markdown 和 Asciidoctor。

  Emacs Org-mode 很快也会支持。

* 用户可以在 [设置](/settings#languages) 里选择自己想看到的文章语言，比如中文和英文。

### FAQ

* Putchar 团队有哪些人？

  目前只有我一人，@tiensonqin。

* 完全免费的话，你怎么赚钱？

  1. 我不会出售你的任何个人信息。
  2. 网站只会在首页右侧放置广告，比如社区赞助。

* Putchar 用了什么技术？

  前端: [Clojurescript](https://clojurescript.org/), [React](https://reactjs.org/).

  后端: [Clojure](https://clojure.org/), Postgres, Redis.

* 网站开源吗？

  https://github.com/tiensonqin/putchar.
"
   :misc "其他设置"
   :dont-show-vote-numbers "不显示顶数量"
   :undo "取消"
   :required "需要填写"
   :login-to-comment "登录以后才可以评论。"
   :go-to-fullscreen "进入全屏"
   :select-primary-language "选择文章主要语言"
   :select-primary-language-explain "不关注该语言的用户就不会看到这篇文章"
   :select-which-languages "设置你关注的语言。"
   :go-to-home "首页"
   :signin-with-email "邮箱登录"
   :light-theme "浅色主题"
   :dark-theme "深色主题"
   :no-stats-yet "暂时没有统计数据。"
   :recent-7-days "最近 7 天数据"
   :views "浏览"
   :reads "阅读"
   :stats "统计"
   :posts "文章"
   :language-default-choice "设置 \"%s\" 为默认选择"
   :language-must-be-chosen "请选择主要语言!"
   :post-primary-language "文章主要语言"
   :is-my-default-language-choice " 是我的默认文章语言, "
   :change-it-to "修改为 "
   :send-us-an-email-moderator "如果你有兴趣做管理员，可以在 Discord 里留言或者发送给我们邮件。"
   :comments "条评论"
   :share-news "分享文章，论文或者你正在读的书..."
   :name "名称"
   :authors "作者"
   :authors-placeholder "支持 Markdown"
   :change "修改"
   :show-all "显示所有"
   :no-votes-yet "暂时还没有顶的文章。"
   :books "书"
   :book "书"
   :add-a-book "添加一本书"
   :website "网站"
   :introduction "查看介绍"
   :moderation-logs "管理日志"
   })
