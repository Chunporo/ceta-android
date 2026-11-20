package com.ceta.cinema.ui.component

import androidx.annotation.DrawableRes
import com.ceta.cinema.R

/**
 * Stock photos from the design prototype (Unsplash licence: free for commercial use), bundled so the app looks like the design
 * while it is in development. Nothing is fetched from Unsplash at run time.
 * TODO(release): replace with CETA's own product shots, cinema photos and banners (a real image URL from the API always wins).
 */
object DevPhotos {
    /** Product photo of a snack combo, by its code. */
    @DrawableRes fun snack(code: String?): Int? = when (code?.uppercase()) {
        "SOLO" -> R.drawable.snack_solo
        "COUPLE" -> R.drawable.snack_couple
        "FAMILY" -> R.drawable.snack_family
        "CARAMEL" -> R.drawable.snack_caramel
        "DRINK" -> R.drawable.snack_drink
        else -> null
    }

    /** Background of an offer banner, by the offer's code. */
    @DrawableRes fun promo(code: String?): Int? = when (code?.uppercase()) {
        "TUE50" -> R.drawable.promo_1
        "CETA20" -> R.drawable.promo_2
        "HSSV49" -> R.drawable.promo_3
        "IMAXWKND" -> R.drawable.promo_4
        else -> null
    }

    /** Background of an onboarding page (0..2). */
    @DrawableRes fun onboarding(page: Int): Int = when (page) { 0 -> R.drawable.ob_0; 1 -> R.drawable.ob_1; else -> R.drawable.ob_2 }
}
