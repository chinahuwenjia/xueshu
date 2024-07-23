<template>
  <div class="faq-container">
    <h2>常见问题 FAQ</h2>
    <div class="accordion">
      <div v-for="(faq, index) in faqs" :key="index" class="accordion-item">
        <button class="accordion-control" type="button" @click="toggleFaq(index)">
          <div class="accordion-label">{{ faq.question }}</div>
          <div class="accordion-chevron" :class="{ open: faq.open }">
            <svg viewBox="0 0 15 15" fill="none" xmlns="http://www.w3.org/2000/svg" width="16" height="16">
              <path d="M3.13523 6.15803C3.3241 5.95657 3.64052 5.94637 3.84197 6.13523L7.5 9.56464L11.158 6.13523C11.3595 5.94637 11.6759 5.95657 11.8648 6.15803C12.0536 6.35949 12.0434 6.67591 11.842 6.86477L7.84197 10.6148C7.64964 10.7951 7.35036 10.7951 7.15803 10.6148L3.15803 6.86477C2.95657 6.67591 2.94637 6.35949 3.13523 6.15803Z" fill="currentColor" fill-rule="evenodd" clip-rule="evenodd"></path>
            </svg>
          </div>
        </button>
        <div v-if="faq.open" class="accordion-panel">
          <div class="panel-content">{{ faq.answer }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  data() {
    return {
      faqs: [
        {
          question: "本站论文查重系统资质？",
          answer: "国际版论文直接提供至 国际官网 检测并提供查重结果；UK版论文检测直接提交至 UK官网 检测并提供查重结果；检测过程无需人工干预，无时差，真正实现全天24小时自助检测。",
          open: false
        },
        {
          question: "你们如何处理上传的文件？",
          answer: "未经用户同意，上传的文件未被纳入我们的数据库。请注意，其他剽窃工具（尤其是免费的）会将您的论文纳入其数据库。",
          open: false
        },
        {
          question: "我们教授是否知道我用过你们的查重系统？",
          answer: "不会，我们上传是无存储库，系统不会保留任何文档。",
          open: false
        },
        {
          question: "论文安全吗，有使用限制吗？",
          answer: "论文检测过程无需人工干预，无时差，提交成功后直达官网比对，由Turnitin 官网直接出检测报告，安全可靠。实现 7*24 小时检测，无限制全球 140 个国家均可以使用。",
          open: false
        },
        {
          question: "会不会收录，导致下次 100% 相似率？",
          answer: "本自助系统提交的是无存储库，所以你的论文不会被 Turnitin 数据库收录，请放心使用。无论检测一次还是检测多次都不会被收录入库。",
          open: false
        },
        {
          question: "关于 Turnitin 国际版与 TurnitinUK 版有什么区别？该怎么选择？",
          answer: "国际版与 UK 的学生数据库有差别，因此检测的结果会不一样。国际版：使用范围最大，中国、美国、加拿大、澳洲、美洲、亚洲、欧洲（除英国外）、非洲、香港、澳门、台湾等世界上 126 个国家的学校都是使用这个系统来检测；UK 版：只有英国（UK）的大学使用。UK 是专为 UK 大学开发的，90% 以上的 UK 大学都是使用 UK 系统；如果是在英国（UK）留学的请选择 UK 来检测，在其他国家留学的请选择 国际版检测。也就是在英国留学的同学一定要选择 UK 系统，不是在英国留学的都是选择 国际版；如果需要到国际期刊（如EI、SCI）上发表论文，需要提前检测，请选择 国际版检测，国际版还支持多语种检测，如中文、日语、西班牙语、德语、俄语、法语等小语种；目前中国也有部分大学引进了 国际版系统来检测外语系的论文。国内大学的中文学位论文检测，请使用国际版检测。",
          open: false
        },
        {
          question: "提交文档大小以及格式问题？",
          answer: "单个档案大小必须小于 20 MB (建议控制在10MB以内)；档案的内文必须有至少 100个字；文稿的长度不得超过 400 页；允许的文档类型: PDF、DOC、DOCX、TXT。",
          open: false
        },
        {
          question: "AI 查重的限制和要求？",
          answer: "AI检测文档需要满足以下要求：1、目前 AI 检测只支持英文文档。2、文档字数必须在 300-15000 字内。3、文档大小必须小于 100 MB。接受的文件类型：.docx、.pdf、.txt、.rtf",
          open: false
        }
      ]
    };
  },
  methods: {
    toggleFaq(index) {
      this.faqs.forEach((faq, i) => {
        faq.open = i === index ? !faq.open : false;
      });
    }
  }
};
</script>

<style>
.faq-container {
  width: 100%;
  max-width: 800px;
  margin: auto;
  padding: 20px;
  background-color: #ffffff;
  border-radius: 10px;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
}

h2 {
  text-align: center;
  margin-bottom: 20px;
  color: #333;
}

.accordion {
  width: 100%;
}

.accordion-item {
  margin-bottom: 10px;
  border-bottom: 1px solid #e0e0e0;
  padding-bottom: 10px;
}

.accordion-control {
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #f9f9f9;
  padding: 10px 20px;
  border: none;
  border-radius: 5px;
  cursor: pointer;
  transition: background 0.3s ease, color 0.3s ease;
  color: #333;
  font-size: 16px;
  font-weight: bold;
}

.accordion-control:hover {
  background: #e0e0e0;
}

.accordion-chevron {
  transition: transform 0.3s;
}

.accordion-chevron.open {
  transform: rotate(180deg);
}

.accordion-panel {
  overflow: hidden;
  transition: max-height 0.3s ease-out;
}

.panel-content {
  padding: 10px 20px;
  background: #fafafa;
  border-radius: 5px;
  margin-top: 10px;
  color: #666;
  font-size: 14px;
  line-height: 1.6;
}
</style>
