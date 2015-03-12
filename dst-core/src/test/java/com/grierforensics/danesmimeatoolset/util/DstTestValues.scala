package com.grierforensics.danesmimeatoolset.util

import javax.mail.internet.InternetAddress

import com.grierforensics.danesmimeatoolset.TestDaneSmimeaService
import com.grierforensics.danesmimeatoolset.util.ConfigHolder._
import org.bouncycastle.pkix.jcajce.JcaPKIXIdentity

object DstTestValues {

  val testDss = new TestDaneSmimeaService

  val emailWithoutDane = "a@example.com"

  val emailWithDane = "danetest@had-pilot.biz"
  val emailWithDanePublishedZoneLine = """b14e06132f0c359080e7f2f55946995f22a6ad14e0528c48906fb128._smimecert.had-pilot.biz. 299 IN TYPE65500 \# 1058 0300003082041B30820303A003020102020900B1F478FC6480FE7C300D06092A864886F70D01010505003081A3310B3009060355040613025553310E300C06035504080C0553746174653115301306035504070C0C44656661756C74204369747931123010060355040A0C0944414E4520546573743111300F060355040B0C0864616E6574657374311F301D06035504030C1664616E6574657374406861642D70696C6F742E62697A3125302306092A864886F70D010901161664616E6574657374406861642D70696C6F742E62697A301E170D3135303132323139353231355A170D3135303232313139353231355A3081A3310B3009060355040613025553310E300C06035504080C0553746174653115301306035504070C0C44656661756C74204369747931123010060355040A0C0944414E4520546573743111300F060355040B0C0864616E6574657374311F301D06035504030C1664616E6574657374406861642D70696C6F742E62697A3125302306092A864886F70D010901161664616E6574657374406861642D70696C6F742E62697A30820122300D06092A864886F70D01010105000382010F003082010A028201010096CA3B7C2610F79F40318BD616737C5D70A5F2B806795E8CDF5A69491AD78060A3501F364CDC3A61C491126AE21EA00D973B0C4D9DE635C1A844BDF5AEF662310E848C1F03456553BFBAB2EF04917DC9C5C24FC2217EC590B958EC83887A6AF239A00A38F1F1E4CB7814D49CCFA36977C2E77239E79759D79A3D7A448E462414C73DE88CDEF61133B0087297AEB39761E7270A8292C711A404D258614DC58489D74BF1BADC5DF0C8472086B3C3F828D947B96C48149E59C47A1833F13A4FBB31E91D34F0AEA51A100F982219E7447D94AF0DA0DB811B41E1933367EF11618E0318C2EC29ACE0206FF943973F694A8F05B8E11F414F00A0FBCD7C2511BA13A3750203010001A350304E301D0603551D0E0416041424C50BBBEE31C6AD9A7AEC845BD0CC435FF4FA2B301F0603551D2304183016801424C50BBBEE31C6AD9A7AEC845BD0CC435FF4FA2B300C0603551D13040530030101FF300D06092A864886F70D010105050003820101001497E04E8629172C3B545693F3C0C8F2B567CA573C6A6AFD2788B001DC4CEDD87538E8F2EFFF4FDB9F2FBDE43FFC1EBBD32A247138741D2266E146585C77D262F0278E3D860CEAAE44570B81D069D4B893496B61751BA996B6A461B456F63A8FAB51ACEC63897DFFA5B106823E90917CD6247867C206B09E4606390223A100F7F2746B9128FB8E187533F272F1347CCAF4B1E6A16F408816855FBCCE2575193A8121CC68378F4A8A8F82144A27E1D78B7FEE66D6FD63D91A23D2C1CF0811A16B839655B6A0020287C7AA0B1BF97AD4469AE83E8649FFF6C622DD3FB5D9FCAE064EDCE2DFD7F0ADE6AEFC106557B436D9A518F47ED8961622E16A96115E393619"""
  val emailWithDaneCertHex = "3082041B30820303A003020102020900B1F478FC6480FE7C300D06092A864886F70D01010505003081A3310B3009060355040613025553310E300C06035504080C0553746174653115301306035504070C0C44656661756C74204369747931123010060355040A0C0944414E4520546573743111300F060355040B0C0864616E6574657374311F301D06035504030C1664616E6574657374406861642D70696C6F742E62697A3125302306092A864886F70D010901161664616E6574657374406861642D70696C6F742E62697A301E170D3135303132323139353231355A170D3135303232313139353231355A3081A3310B3009060355040613025553310E300C06035504080C0553746174653115301306035504070C0C44656661756C74204369747931123010060355040A0C0944414E4520546573743111300F060355040B0C0864616E6574657374311F301D06035504030C1664616E6574657374406861642D70696C6F742E62697A3125302306092A864886F70D010901161664616E6574657374406861642D70696C6F742E62697A30820122300D06092A864886F70D01010105000382010F003082010A028201010096CA3B7C2610F79F40318BD616737C5D70A5F2B806795E8CDF5A69491AD78060A3501F364CDC3A61C491126AE21EA00D973B0C4D9DE635C1A844BDF5AEF662310E848C1F03456553BFBAB2EF04917DC9C5C24FC2217EC590B958EC83887A6AF239A00A38F1F1E4CB7814D49CCFA36977C2E77239E79759D79A3D7A448E462414C73DE88CDEF61133B0087297AEB39761E7270A8292C711A404D258614DC58489D74BF1BADC5DF0C8472086B3C3F828D947B96C48149E59C47A1833F13A4FBB31E91D34F0AEA51A100F982219E7447D94AF0DA0DB811B41E1933367EF11618E0318C2EC29ACE0206FF943973F694A8F05B8E11F414F00A0FBCD7C2511BA13A3750203010001A350304E301D0603551D0E0416041424C50BBBEE31C6AD9A7AEC845BD0CC435FF4FA2B301F0603551D2304183016801424C50BBBEE31C6AD9A7AEC845BD0CC435FF4FA2B300C0603551D13040530030101FF300D06092A864886F70D010105050003820101001497E04E8629172C3B545693F3C0C8F2B567CA573C6A6AFD2788B001DC4CEDD87538E8F2EFFF4FDB9F2FBDE43FFC1EBBD32A247138741D2266E146585C77D262F0278E3D860CEAAE44570B81D069D4B893496B61751BA996B6A461B456F63A8FAB51ACEC63897DFFA5B106823E90917CD6247867C206B09E4606390223A100F7F2746B9128FB8E187533F272F1347CCAF4B1E6A16F408816855FBCCE2575193A8121CC68378F4A8A8F82144A27E1D78B7FEE66D6FD63D91A23D2C1CF0811A16B839655B6A0020287C7AA0B1BF97AD4469AE83E8649FFF6C622DD3FB5D9FCAE064EDCE2DFD7F0ADE6AEFC106557B436D9A518F47ED8961622E16A96115E393619"

  val dstAddress: InternetAddress = new InternetAddress(config.getString("Workflow.fromAddress"), config.getString("Workflow.fromName"))
  val dstIdentity = IdentityUtil.generateIdentity(dstAddress)
  val testAddress: InternetAddress = dstAddress
  val testIdentity: JcaPKIXIdentity = dstIdentity

  val bobAddress: InternetAddress = new InternetAddress("dst.bob@example.com", "Bob")
  val bobIdentity: JcaPKIXIdentity = IdentityUtil.generateIdentity(bobAddress)
//  val bobPopHost: String = "pop.gmail.com"
//  val bobEmailPassword: String = "dst.bob!"

  val aliceAddress: InternetAddress = new InternetAddress("dst.alice@example.com", "Alice")
  val aliceIdentity: JcaPKIXIdentity = IdentityUtil.generateIdentity(aliceAddress)

  val badCertHex = "F70D010100B1F478FC6480FE7C300D06092A86488603081A3310B3009060355040613025553310E30050503082041B30820303A0030201020209015301306035504070C0C446566617560F060355040B0C0864616E6574657374311F3010A0C0944414E452054657374311130D06035504030C1664616E657465737440686110060355040642D70696C6F742E62697A3125302306092A864886F70D010901161664616E6574657374406861642D70696C6F742E62697A301E170D3135303132323139353231355A170D3135303232313139353231355A3081A3310B3009060355040613025553310E300C06035504080C0553746174653115301306035504070C0C44656661756C74204369747931123010060355040A0C0944414E4520546573743111300F060355040B0C0864616E6574657374311F301D06035504030C1664616E6574657374406861642D70696C6F742E62697A3125302306092A864886F70D010901161664616E6574657374406861642D70696C6F742E62697A30820122300D06092A864886F70D01010105000382010F003082010A028201010096CA3B7C2610F79F40318BD616737C5D70A5F2B806795E8CDF5A69491AD78060A3501F364CDC3A61C491126AE21EA00D973B0C4D9DE635C1A844BDF5AEF662310E848C1F03456553BFBAB2EF04917DC9C5C24FC2217EC590B958EC83887A6AF239A00A38F1F1E4CB7814D49CCFA36977C2E77239E79759D79A3D7A448E462414C73DE88CDEF6113374BF1BADC5DF0C8472086B3C3F828D947B96C48149E59C47A1833F13A4FBB31E91D34F0AEA51A100F982219E7447D94AF0DA0DB811B41E1933367EF11618E0318C2EC29ACE0206FF943973F69B0087297AEB39761E7270A8292C711A404D258614DC58489D13A3750203010001A350304E301D0603551D0E0416041424C50BBBEE31C6AD9A7AEC845BD0CC435FF4FA2B301F0603551D2304183016801424C50BBBEE31C6AD9A7AEC845BD0CC435FF4FA2B300C0603551D13040530030101FF300D06092A864886F70D010105050003820101001497E04E8629172C3B545693F3C0C8F2B567CA573C6A6AFD2788B001DC4CEDD87538E8F2EFFF4FDB9F2FBDE43FFC1EBBD32A247138741D2266E146585C77D262F0278E3D860CEAAE44570B81D069D4B893496B61751BA996B6A461B456F63A8FAB51ACEC63897DFFA5B106823E90917CD6247867C206B09E4606390223A100F7F2746B9128FB8E187533F272F1347CCAF4B1E6A16F408816855FBCCE2575193A8121CC68378F4A8A8F82144A27E1D78B7FEE66D6FD63D91A23D2C1CF0811A16B839655B6A0020287C7AA0B1BF97AD4469AE83E8649FFF6C622DD3FB5D9FCAE064EDCE2DFD7F0ADE6AEFC106557B436D9A518F47ED8961622E16A96115E393619"
}
